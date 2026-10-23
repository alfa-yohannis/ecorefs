package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One call between two files: a call activity of the caller file names a
 * process of the callee file by its identifier. The cleaning of the dataset
 * removed these {@code calledElement} values, and {@code resolve_called_elements.py}
 * found the file that defines each called process.
 */
final class CallLink {

    final String caller;
    final String activity;
    final String called;
    final String callee;

    /**
     * Keeps one link.
     *
     * @param caller   the caller file, relative to the dataset folder
     * @param activity the identifier of the call activity
     * @param called   the identifier of the called process
     * @param callee   the callee file, relative to the dataset folder
     */
    private CallLink(String caller, String activity, String called, String callee) {
        this.caller = caller;
        this.activity = activity;
        this.called = called;
        this.callee = callee;
    }

    /**
     * Reads the links from {@code called-element-links.tsv}.
     *
     * @param table the TSV file with the columns collection, caller, activity,
     *              called, callee and rule
     * @return the links
     * @throws IOException when the table cannot be read
     */
    static List<CallLink> read(Path table) throws IOException {
        List<CallLink> links = new ArrayList<>();
        List<String> lines = Files.readAllLines(table, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            links.add(new CallLink(columns[1], columns[2], columns[3], columns[4]));
        }
        return links;
    }
}
