package uk.ac.ucl.rits.inform.datasources.ids.hl7.parser;

import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import lombok.Getter;
import uk.ac.ucl.rits.inform.datasources.ids.exceptions.Hl7InconsistencyException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Parses notes, constructors for populating questions and comments, or just comments.
 */
public class NotesParser {

    private final List<NTE> notes;
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
     * @throws Hl7InconsistencyException if answer with no question encountered
     */
    public NotesParser(Collection<NTE> notes, String questionSeparator, Pattern questionPattern) throws Hl7InconsistencyException {
        this.notes = List.copyOf(notes);
        this.questionSeparator = questionSeparator;
        this.questionPattern = questionPattern;
        int numberOfNotes = notes.size();
        questions = new HashMap<>(numberOfNotes);
        buildQuestionsAndComments();
    }

    public NotesParser(Collection<NTE> notes) {
        this.notes = List.copyOf(notes);
        buildComments();
    }

    private void buildComments() {
        StringJoiner notesBuilder = new StringJoiner("\n");
        for (NTE note : notes) {
            addSubCommentsFromNote(notesBuilder, note);
        }
        comments = notesBuilder.toString().strip();
    }


    private void buildQuestionsAndComments() throws Hl7InconsistencyException {
        String previousQuestion = null;
        StringJoiner commentJoiner = new StringJoiner("\n");

        for (NTE note : notes) {
            StringJoiner questionJoiner = new StringJoiner("\n");
            addSubCommentsFromNote(questionJoiner, note);
            String noteComments = questionJoiner.toString().strip();

            boolean currentIsNotAQuestion = !questionPattern.matcher(noteComments).find();
            if (previousQuestion == null && currentIsNotAQuestion) {
                commentJoiner.add(noteComments);
            } else {
                previousQuestion = addQuestionAndAnswerReturningQuestion(noteComments, previousQuestion);
            }
            comments = commentJoiner.toString().strip();
        }
    }

    private String addQuestionAndAnswerReturningQuestion(String joinedComments, String previousQuestion) throws Hl7InconsistencyException {
        String question = previousQuestion;
        String[] parts = questionPattern.split(joinedComments, -1);
        if (parts.length == 1) {
            concatenateAnswerAndSaveToQuestions(question, joinedComments);
        } else {
            question = parts[0];
            // Separator can be in the answer so join it back in
            String answer = String.join(questionSeparator, Arrays.copyOfRange(parts, 1, (parts.length)));
            concatenateAnswerAndSaveToQuestions(question, answer);
        }
        if (question == null || question.isEmpty()) {
            throw new Hl7InconsistencyException("Null question encountered");
        }
        return question;
    }

    private void concatenateAnswerAndSaveToQuestions(String question, String answer) {
        String outputAnswer = answer;
        if (questions.containsKey(question)) {
            outputAnswer = String.format("%s\n%s", questions.get(question), answer).trim();
        }
        questions.put(question, outputAnswer);
    }

    private void addSubCommentsFromNote(StringJoiner commentJoiner, NTE note) {
        for (FT ft : note.getNte3_Comment()) {
            commentJoiner.add(ft.getValueOrEmpty().strip());
        }
    }
}
