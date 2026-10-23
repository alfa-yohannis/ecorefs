package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.KuboNode;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.eclipse.bpmn2.Bpmn2Package;

/**
 * Entry point of the RQ1 experiment on real BPMN models. Four commands exist:
 *
 * <pre>
 * roundtrip  &lt;kubo&gt; &lt;dataset&gt; &lt;temporary-folder&gt; &lt;collection&gt; &lt;list&gt; &lt;output.tsv&gt; [timeout-seconds] [limit]
 * statistics &lt;kubo&gt; &lt;dataset&gt; &lt;collection&gt; &lt;list&gt; &lt;output.tsv&gt;
 * dedup      &lt;kubo&gt; &lt;dataset&gt; &lt;output.tsv&gt; &lt;collection&gt;=&lt;list&gt; ...
 * directory  &lt;kubo&gt; &lt;dataset&gt; &lt;output.tsv&gt; &lt;collection&gt;=&lt;list&gt; ...
 * </pre>
 *
 * {@code roundtrip} saves and loads every model of one collection through seven
 * persistence paths. {@code statistics} only counts the properties of the models.
 * {@code dedup} adds every file to a fresh node, file by file. {@code directory}
 * adds each collection as one directory to a fresh node. {@code kubo} is the
 * multiaddress of the API of the node, which the IPFS client contacts when it starts.
 */
public final class Rq1Main {

    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    /**
     * Prevents instances; the class is the entry point only.
     */
    private Rq1Main() {
    }

    /**
     * Runs one command.
     *
     * @param arguments the command and its arguments
     * @throws IOException when a list, a model or an output file cannot be read or written
     */
    public static void main(String[] arguments) throws IOException {
        Bpmn2Package.eINSTANCE.getName();
        if (arguments.length < 4) {
            printUsageAndExit();
        }
        KuboNode node = new KuboNode(arguments[1]);
        Path datasetRoot = Path.of(arguments[2]).toAbsolutePath().normalize();
        switch (arguments[0]) {
            case "roundtrip":
                runRoundTrip(node, datasetRoot, arguments);
                break;
            case "statistics":
                runStatistics(node, datasetRoot, arguments);
                break;
            case "dedup":
                new DeduplicationRun(node).run(readCollections(datasetRoot, arguments), Path.of(arguments[3]));
                break;
            case "directory":
                new DirectoryPublicationRun(node).run(readCollections(datasetRoot, arguments), Path.of(arguments[3]));
                break;
            default:
                printUsageAndExit();
        }
    }

    /**
     * Runs the round trip over one collection.
     *
     * @param node        the node of the experiment
     * @param datasetRoot the folder of the collections
     * @param arguments   the command line
     * @throws IOException when the list or the output cannot be read or written
     */
    private static void runRoundTrip(KuboNode node, Path datasetRoot, String[] arguments) throws IOException {
        if (arguments.length < 7) {
            printUsageAndExit();
        }
        Path temporaryFolder = Files.createDirectories(Path.of(arguments[3]).toAbsolutePath().normalize());
        long timeoutSeconds = arguments.length > 7 ? Long.parseLong(arguments[7]) : DEFAULT_TIMEOUT_SECONDS;
        int limit = arguments.length > 8 ? Integer.parseInt(arguments[8]) : 0;
        ModelList models = ModelList.read(datasetRoot, arguments[4], Path.of(arguments[5]), limit);
        ExperimentResourceSets resourceSets = new ExperimentResourceSets(node.client(),
                List.of(datasetRoot, temporaryFolder));
        List<PersistencePath> paths = createPaths(resourceSets, node, temporaryFolder);
        new RoundTripRun(models, new SourceModelLoader(resourceSets), paths, timeoutSeconds)
                .run(Path.of(arguments[6]));
    }

    /**
     * Counts the properties of every model of one collection.
     *
     * @param node        the node of the experiment
     * @param datasetRoot the folder of the collections
     * @param arguments   the command line
     * @throws IOException when the list or the output cannot be read or written
     */
    private static void runStatistics(KuboNode node, Path datasetRoot, String[] arguments) throws IOException {
        if (arguments.length < 6) {
            printUsageAndExit();
        }
        ModelList models = ModelList.read(datasetRoot, arguments[3], Path.of(arguments[4]), 0);
        ExperimentResourceSets resourceSets = new ExperimentResourceSets(node.client(), List.of(datasetRoot));
        new StatisticsRun(models, new SourceModelLoader(resourceSets)).run(Path.of(arguments[5]));
    }

    /**
     * Creates the seven persistence paths in the order of the result table.
     *
     * @param resourceSets    the builder of the resource sets
     * @param node            the node of the experiment
     * @param temporaryFolder the folder for local files
     * @return the paths
     */
    private static List<PersistencePath> createPaths(ExperimentResourceSets resourceSets, KuboNode node,
            Path temporaryFolder) {
        List<PersistencePath> paths = new ArrayList<>();
        paths.add(new LocalXmiPath("xmi_local", resourceSets, false, temporaryFolder));
        paths.add(new IpfsXmiPath("xmi_ipfs", resourceSets, false));
        paths.add(new LocalXmiPath("emd_local", resourceSets, true, temporaryFolder));
        paths.add(new IpfsXmiPath("emd_ipfs", resourceSets, true));
        paths.add(new LocalBpmnXmlPath("bpmn_local", resourceSets, temporaryFolder));
        paths.add(new IpfsBpmnXmlPath("bpmn_ipfs", resourceSets, node, false));
        paths.add(new IpfsBpmnXmlPath("bpmn_ipfs_numbered", resourceSets, node, true));
        return paths;
    }

    /**
     * Reads the collections named on the command line as {@code collection=list}.
     *
     * @param datasetRoot the folder of the collections
     * @param arguments   the command line, with the collections from the fifth argument on
     * @return the model lists
     * @throws IOException when a list cannot be read
     */
    private static List<ModelList> readCollections(Path datasetRoot, String[] arguments) throws IOException {
        List<ModelList> collections = new ArrayList<>();
        for (int index = 4; index < arguments.length; index++) {
            String[] parts = arguments[index].split("=", 2);
            collections.add(ModelList.read(datasetRoot, parts[0], Path.of(parts[1]), 0));
        }
        return collections;
    }

    /**
     * Prints the usage and stops the virtual machine.
     */
    private static void printUsageAndExit() {
        System.err.println("usage: roundtrip <kubo> <dataset> <temporary-folder> <collection> <list> <output.tsv>"
                + " [timeout-seconds] [limit]");
        System.err.println("       statistics <kubo> <dataset> <collection> <list> <output.tsv>");
        System.err.println("       dedup <kubo> <dataset> <output.tsv> <collection>=<list> ...");
        System.err.println("       directory <kubo> <dataset> <output.tsv> <collection>=<list> ...");
        System.exit(2);
    }
}
