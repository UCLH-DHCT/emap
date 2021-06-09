package uk.ac.ucl.rits.inform.datasources.ids.hl7.parser;

import ca.uhn.hl7v2.model.v26.datatype.FT;
import ca.uhn.hl7v2.model.v26.segment.NTE;
import lombok.Getter;

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
     */
    public NotesParser(Collection<NTE> notes, String questionSeparator, Pattern questionPattern) {
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


    private void buildQuestionsAndComments() {
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

    private String addQuestionAndAnswerReturningQuestion(String joinedComments, String previousQuestion) {
        String[] parts = questionPattern.split(joinedComments);
        if (parts.length == 1) {
            concatenateAnswerAndSaveToQuestions(previousQuestion, joinedComments);
        } else {
            previousQuestion = parts[0];
            // allow for separator to be in the answer
            String answer = String.join(questionSeparator, Arrays.copyOfRange(parts, 1, (parts.length)));
            concatenateAnswerAndSaveToQuestions(previousQuestion, answer);
        }
        return previousQuestion;
    }

    private void concatenateAnswerAndSaveToQuestions(String question, String answer) {
        if (questions.containsKey(question)) {
            answer = String.format("%s\n%s", questions.get(question), answer);
        }
        questions.put(question, answer);
    }

    private void addSubCommentsFromNote(StringJoiner commentJoiner, NTE note) {
        for (FT ft : note.getNte3_Comment()) {
            commentJoiner.add(ft.getValueOrEmpty().strip());
        }
    }
}
