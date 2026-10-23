package org.ecorefs.eval.bpmn;

/**
 * Turns exceptions and messages into short single-line texts for the columns of
 * the result tables.
 */
public final class Failures {

    private static final int MAXIMUM_LENGTH = 160;

    /**
     * Prevents instances; the class offers static operations only.
     */
    private Failures() {
    }

    /**
     * Describes the root cause of a failure by class name and message.
     *
     * @param failure the exception or error
     * @return the description on one line
     */
    public static String describe(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return clean(root.getClass().getSimpleName() + ": " + root.getMessage());
    }

    /**
     * Removes tabs and line breaks and shortens long texts.
     *
     * @param text the text, or null
     * @return the cleaned text
     */
    public static String clean(String text) {
        return clean(text, MAXIMUM_LENGTH);
    }

    /**
     * Removes tabs and line breaks and shortens the text to a given length.
     *
     * @param text          the text, or null
     * @param maximumLength the largest length to keep
     * @return the cleaned text
     */
    public static String clean(String text, int maximumLength) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replaceAll("[\\t\\r\\n]+", " ");
        return singleLine.length() > maximumLength ? singleLine.substring(0, maximumLength) : singleLine;
    }
}
