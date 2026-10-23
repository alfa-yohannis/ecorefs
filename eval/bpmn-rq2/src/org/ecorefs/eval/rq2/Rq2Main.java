package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.ecorefs.eval.bpmn.KuboNode;
import org.eclipse.bpmn2.Bpmn2Package;

/**
 * Entry point of the RQ2 experiments on real BPMN models:
 *
 * <pre>
 * split &lt;kubo&gt; &lt;dataset&gt; &lt;multi-pool-models.tsv&gt; &lt;output.tsv&gt; [limit]
 * calls &lt;kubo&gt; &lt;dataset&gt; &lt;called-element-links.tsv&gt; &lt;output.tsv&gt;
 * </pre>
 *
 * {@code kubo} is the multiaddress of the API of the node, and {@code dataset}
 * the folder of the cleaned collections.
 */
public final class Rq2Main {

    /**
     * Prevents instances; the class is the entry point only.
     */
    private Rq2Main() {
    }

    /**
     * Runs one command.
     *
     * @param arguments the command and its arguments
     * @throws IOException when a table cannot be read or written
     */
    public static void main(String[] arguments) throws IOException {
        Bpmn2Package.eINSTANCE.getName();
        if (arguments.length < 5) {
            printUsageAndExit();
        }
        KuboNode node = new KuboNode(arguments[1]);
        Path datasetRoot = Path.of(arguments[2]).toAbsolutePath().normalize();
        Path input = Path.of(arguments[3]);
        Path output = Path.of(arguments[4]);
        if ("split".equals(arguments[0])) {
            int limit = arguments.length > 5 ? Integer.parseInt(arguments[5]) : 0;
            new SplitExperiment(node, datasetRoot).run(readModels(input, limit), output);
        } else if ("calls".equals(arguments[0])) {
            new CallGraphExperiment(node, datasetRoot).run(CallLink.read(input), output);
        } else {
            printUsageAndExit();
        }
    }

    /**
     * Reads the models with two or more pools.
     *
     * @param table the TSV file with the columns collection, file, pools and processes
     * @param limit the largest number of models to keep, or 0 for all
     * @return the rows without the header
     * @throws IOException when the table cannot be read
     */
    private static List<String[]> readModels(Path table, int limit) throws IOException {
        List<String> lines = Files.readAllLines(table, StandardCharsets.UTF_8);
        List<String[]> models = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (limit == 0 || models.size() < limit) {
                models.add(line.split("\t", -1));
            }
        }
        return models;
    }

    /**
     * Prints the usage and stops the virtual machine.
     */
    private static void printUsageAndExit() {
        System.err.println("usage: split <kubo> <dataset> <multi-pool-models.tsv> <output.tsv> [limit]");
        System.err.println("       calls <kubo> <dataset> <called-element-links.tsv> <output.tsv>");
        System.exit(2);
    }
}
