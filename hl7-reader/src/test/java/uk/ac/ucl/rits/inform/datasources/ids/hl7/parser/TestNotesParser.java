package uk.ac.ucl.rits.inform.datasources.ids.hl7.parser;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ORM_O01;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ucl.rits.inform.datasources.ids.HL7Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class TestNotesParser {

    private static final String PATH_TEMPLATE = "NotesParser/%s.txt";
    private static final String QUESTION_SEPARATOR = "->";
    private static final Pattern QUESTION_PATTERN = Pattern.compile(QUESTION_SEPARATOR);


    List<NTE> getNotesFromORMO01(String resourceFileName) throws HL7Exception, IOException {
        String hl7 = HL7Utils.readHl7FromResource(String.format(PATH_TEMPLATE, resourceFileName));
        ORM_O01 hl7Msg = (ORM_O01) HL7Utils.parseHl7String(hl7);
        return hl7Msg.getORDER().getORDER_DETAIL().getNTEAll();
    }

    List<NTE> getNotesFromFirstOruR01Result(String resourceFileName) throws HL7Exception, IOException {
        String hl7 = HL7Utils.readHl7FromResource(String.format(PATH_TEMPLATE, resourceFileName));
        ORU_R01 hl7Msg = (ORU_R01) HL7Utils.parseHl7String(hl7);
        return hl7Msg.getPATIENT_RESULT().getORDER_OBSERVATION().getNTEAll();
    }

    /**
     * Comment spanning multiple NTEs and questions afterwards should be parsed into comments and questions.
     * @throws Exception shouldn't happen
     */
    @Test
    void testCommentAndQuestions() throws Exception {
        List<NTE> notes = getNotesFromORMO01("comment_and_questions");
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);

        assertEquals(3, parser.getQuestions().size());
        assertEquals("Admitted with delirium vs cognitive decline\nLives alone", parser.getComments());
    }

    /**
     * 3 questions, second question has answer in same note, which continues in following note. This should be concatenated into the same answer.
     * NTE|2||Did you contact the team?->No
     * NTE|3||*** attempted 3x
     * @throws Exception shouldn't happen
     */
    @Test
    void testMultiLineAnswer() throws Exception {
        List<NTE> notes = getNotesFromORMO01("multiline_answer");
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);

        Map<String, String> questions = parser.getQuestions();

        assertEquals(3, questions.size());
        assertEquals("No\n*** attempted 3x", questions.get("Did you contact the team?"));
    }

    /**
     * 2 questions, first question repeated with different answers across notes, these should be concatenated into the same answer.
     * NTE|4||Reason for Consult:->Baby > 24 hours age
     * NTE|5||Reason for Consult:->Maternal procedure for removal of placenta
     * NTE|6||Reason for Consult:->Milk supply issues
     * NTE|7||Supportive measures already undertaken:->Information given about protecting supply
     * @throws Exception shouldn't happen
     */
    @Test
    void testRepeatQuestion() throws Exception {
        List<NTE> notes = getNotesFromORMO01("repeat_question");
        NotesParser parser = new NotesParser(notes, QUESTION_SEPARATOR, QUESTION_PATTERN);

        Map<String, String> questions = parser.getQuestions();

        assertEquals(2, questions.size());
        assertEquals("Baby > 24 hours age\nMaternal procedure for removal of placenta\nMilk supply issues", questions.get("Reason for Consult:"));
    }

    /**
     * When NoteParser is created for only parsing comments, should parse single note.
     * @throws Exception shouldn't happen
     */
    @Test
    void testOnlyCommentsParsed() throws Exception {
        List<NTE> notes = getNotesFromFirstOruR01Result("oru_r01_comment");
        NotesParser parser = new NotesParser(notes);
        assertEquals("Probable prophylactic anti-D known previously but not detected in this sample.", parser.getComments());
    }

    /**
     * NoteParser initialised for parsing comments, notes should be concatenated to form a single comment
     * @throws Exception shouldn't happen
     */
    @Test
    void testOnlyMultiLineCommentsParsed() throws Exception {
        List<NTE> notes = getNotesFromFirstOruR01Result("oru_r01_multiline_comment");
        NotesParser parser = new NotesParser(notes);
        assertEquals("Comment\nspans\nmultiple lines", parser.getComments());
    }

    /**
     * NoteParser initialised for parsing comments, sub-notes should be concatenated to form a single comment
     * @throws Exception shouldn't happen
     */
    @Test
    void testSubCommentsParsed() throws Exception {
        List<NTE> notes = getNotesFromFirstOruR01Result("oru_r01_sub_comments");
        NotesParser parser = new NotesParser(notes);
        assertEquals("Clinical Note1\nover 2 lines\nClinical Note2", parser.getComments());
    }
}
