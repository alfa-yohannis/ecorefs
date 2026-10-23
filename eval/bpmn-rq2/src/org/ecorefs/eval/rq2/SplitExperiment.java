package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.KuboNode;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.ecorefs.eval.bpmn.VendorExtensions;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * RQ2 on real model structures: every model with two or more pools is split into
 * one resource per process, one for the collaborations with the diagrams and one
 * for the other root elements, published on IPFS and changed twice. The first
 * change renames the first process, the second renames the first named element of
 * the shared resource, and each change runs one cascade save.
 */
final class SplitExperiment {

    /** Columns of a model before the columns of the two changes. */
    static final List<String> MODEL_COLUMNS = List.of("collection", "file", "pools", "processes", "resources",
            "edges", "cyclic", "removed_extensions", "publish", "publish_ms", "initial_consistent");

    private static final String NOT_RUN = "-";

    private final KuboNode node;
    private final Path datasetRoot;
    private final ExperimentResourceSets resourceSets;
    private final ExperimentResourceSets loadResourceSets;
    private final SourceModelLoader loader;

    /**
     * Prepares the experiment.
     *
     * @param node        the node of the experiment, offline with an empty repository
     * @param datasetRoot the folder of the cleaned collections
     */
    SplitExperiment(KuboNode node, Path datasetRoot) {
        this.node = node;
        this.datasetRoot = datasetRoot;
        this.resourceSets = new ExperimentResourceSets(node.client(), List.of(datasetRoot));
        this.loadResourceSets = new ExperimentResourceSets(node.client(), List.of());
        this.loader = new SourceModelLoader(resourceSets);
    }

    /**
     * Runs every model of the list and writes one row per model.
     *
     * @param models the rows of {@code multi-pool-models.tsv}: collection, file, pools, processes
     * @param output the TSV file
     * @throws IOException when the table cannot be written
     */
    void run(List<String[]> models, Path output) throws IOException {
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println(String.join("\t", header()));
            int done = 0;
            for (String[] model : models) {
                table.println(String.join("\t", runOne(model[0], model[1], model[2])));
                if (++done % 100 == 0) {
                    table.flush();
                    System.err.println("split: " + done + "/" + models.size());
                }
            }
        }
    }

    /**
     * Splits, publishes and changes one model.
     *
     * @param collection the collection of the model
     * @param file       the path of the model relative to the dataset folder
     * @param pools      the number of pools with a process
     * @return the row of the model
     */
    private List<String> runOne(String collection, String file, String pools) {
        List<String> row = new ArrayList<>(List.of(collection, file, pools));
        try {
            Resource source = loader.load(datasetRoot.resolve(file));
            int removed = VendorExtensions.strip(source);
            SplitModel split = SplitModel.split(source, resourceSets.forIpfsXmi());
            ReferenceGraph graph = ReferenceGraph.of(split.resources);
            row.addAll(List.of(Integer.toString(split.processes.size()), Integer.toString(split.resources.size()),
                    Integer.toString(graph.edgeCount()), graph.hasCycle() ? "yes" : "no", Integer.toString(removed)));
            long publishNanos = graph.publishAll();
            row.addAll(List.of("OK", CascadeTrial.millis(publishNanos)));
            runChanges(split, graph, row);
        } catch (Exception | StackOverflowError e) {
            row.add("FAIL " + Failures.describe(e));
        }
        return padded(row);
    }

    /**
     * Checks the first publication and runs the two changes.
     *
     * @param split the split model, published
     * @param graph the reference graph of the parts
     * @param row   the row under construction
     * @throws IOException when a save or a read on the node fails
     */
    private void runChanges(SplitModel split, ReferenceGraph graph, List<String> row) throws IOException {
        CidHistory history = new CidHistory();
        history.record(split.resources);
        row.add(SnapshotCheck.run(node, split.resources, history).consistent() ? "yes" : "no");
        CascadeTrial trial = new CascadeTrial(node, split.resources, graph, history, loadResourceSets);
        Resource process = split.processes.get(0);
        EObject processObject = NameChange.firstNamedObject(process, Process.class);
        row.addAll(processObject == null ? notRun() : trial.run(process, processObject, split.collaboration));
        EObject sharedObject = split.shared == null ? null : NameChange.firstNamedObject(split.shared, RootElement.class);
        row.addAll(sharedObject == null ? notRun() : trial.run(split.shared, sharedObject, split.collaboration));
    }

    /**
     * Returns the columns of a change that did not run.
     *
     * @return a dash per column
     */
    private static List<String> notRun() {
        return Collections.nCopies(CascadeTrial.COLUMNS.size(), NOT_RUN);
    }

    /**
     * Fills the remaining columns of a row with dashes.
     *
     * @param row the row
     * @return the full row
     */
    private static List<String> padded(List<String> row) {
        int width = header().size();
        row.addAll(Collections.nCopies(Math.max(0, width - row.size()), NOT_RUN));
        return row;
    }

    /**
     * Returns the column names of the table.
     *
     * @return the model columns, then the columns of the process change and of the shared change
     */
    private static List<String> header() {
        List<String> columns = new ArrayList<>(MODEL_COLUMNS);
        for (String change : List.of("process", "shared")) {
            for (String suffix : CascadeTrial.COLUMNS) {
                columns.add(change + "_" + suffix);
            }
        }
        return columns;
    }
}
