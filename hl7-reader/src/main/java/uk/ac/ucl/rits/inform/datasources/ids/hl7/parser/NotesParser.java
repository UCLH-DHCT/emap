package uk.ac.ucl.rits.inform.datasources.ids.hl7.parser;

import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Parses notes, constructors for populating questions and comments, or just comments.
 */
public class NotesParser {

    private Collection<NTE> notes;
    private String questionSeparator;
    private Pattern questionPattern;
    /**
     * Questions and answers.
     */
    @Getter
    private Map<String, String> questions;
    /**
     * Comments, lines are joined by newline character, each line is trimmed of whitespace.
     */
    @Getter
    private String comments;

    /**
     * Parse notes, populating questions and comments if there are any before the questions.
     * @param notes             notes to be parsed
     * @param questionSeparator question separator as string
     * @param questionPattern   question separator regex
     */
    public NotesParser(Collection<NTE> notes, String questionSeparator, Pattern questionPattern) {
        this.notes = notes;
        this.questionSeparator = questionSeparator;
        this.questionPattern = questionPattern;
        questions = new HashMap<>(notes.size());
        buildQuestionsAndComments();
    }

    public NotesParser(Collection<NTE> notes) {
        this.notes = notes;
        buildComments();
    }

    private void buildComments() {
        StringJoiner notesBuilder = new StringJoiner("\n");
        for (NTE nt : notes) {
            for (FT ft : nt.getNte3_Comment()) {
                notesBuilder.add(ft.getValueOrEmpty().trim());
            }
        }
        comments = notesBuilder.toString().trim();
    }


    private void buildQuestionsAndComments() {
        for (NTE note : notes) {
            StringBuilder questionAndAnswer = new StringBuilder();
            for (FT ft : note.getNte3_Comment()) {
                questionAndAnswer.append(ft.getValueOrEmpty()).append("\n");
            }
            String[] parts = questionPattern.split(questionAndAnswer.toString().strip());
            if (parts.length > 1) {
                String question = parts[0];
                // allow for separator to be in the answer
                String answer = String.join(questionSeparator, Arrays.copyOfRange(parts, 1, (parts.length)));
                questions.put(question, answer);
            }
        }
    }
}
