package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.KuboNode;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.ecorefs.eval.bpmn.VendorExtensions;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * RQ2 on real dependency graphs: the call links between files become references
 * between IPFS resources, one resource per file. A call activity of the caller
 * refers to the called process of the callee through {@code calledElementRef}.
 * Each connected component of files is published, and then every file of the
 * component is changed in turn, each change followed by one cascade save.
 */
final class CallGraphExperiment {

    /** Columns of a change before the columns of the trial. */
    static final List<String> CHANGE_COLUMNS = List.of("component", "files", "links", "links_applied", "edges",
            "cyclic", "publish_ms", "target");

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
    CallGraphExperiment(KuboNode node, Path datasetRoot) {
        this.node = node;
        this.datasetRoot = datasetRoot;
        this.resourceSets = new ExperimentResourceSets(node.client(), List.of(datasetRoot));
        this.loadResourceSets = new ExperimentResourceSets(node.client(), List.of());
        this.loader = new SourceModelLoader(resourceSets);
    }

    /**
     * Runs every component and writes one row per change.
     *
     * @param links  the call links
     * @param output the TSV file
     * @throws IOException when the table cannot be written
     */
    void run(List<CallLink> links, Path output) throws IOException {
        List<List<String>> components = CallComponents.of(links);
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println(String.join("\t", header()));
            for (int index = 0; index < components.size(); index++) {
                for (List<String> row : runComponent(index + 1, components.get(index), links)) {
                    table.println(String.join("\t", row));
                }
                table.flush();
            }
        }
        System.err.println("calls: " + components.size() + " components");
    }

    /**
     * Loads, links, publishes and changes one component.
     *
     * @param number the number of the component
     * @param files  the files of the component
     * @param links  all call links
     * @return one row per change, or one row with the failure
     */
    private List<List<String>> runComponent(int number, List<String> files, List<CallLink> links) {
        List<String> componentColumns = new ArrayList<>(List.of(Integer.toString(number), Integer.toString(files.size())));
        List<List<String>> rows = new ArrayList<>();
        try {
            ResourceSet ipfsResourceSet = resourceSets.forIpfsXmi();
            Map<String, Resource> resourceByFile = loadFiles(files, ipfsResourceSet);
            List<CallLink> componentLinks = linksOf(resourceByFile, links);
            int applied = applyLinks(resourceByFile, componentLinks);
            List<Resource> resources = new ArrayList<>(resourceByFile.values());
            ReferenceGraph graph = ReferenceGraph.of(resources);
            componentColumns.addAll(List.of(Integer.toString(componentLinks.size()), Integer.toString(applied),
                    Integer.toString(graph.edgeCount()), graph.hasCycle() ? "yes" : "no"));
            componentColumns.add(CascadeTrial.millis(graph.publishAll()));
            CidHistory history = new CidHistory();
            history.record(resources);
            CascadeTrial trial = new CascadeTrial(node, resources, graph, history, loadResourceSets);
            for (Map.Entry<String, Resource> entry : resourceByFile.entrySet()) {
                rows.add(runChange(componentColumns, entry.getKey(), entry.getValue(), graph, trial));
            }
        } catch (Exception | StackOverflowError e) {
            List<String> row = new ArrayList<>(componentColumns);
            row.add("FAIL " + Failures.describe(e));
            row.addAll(Collections.nCopies(Math.max(0, header().size() - row.size()), "-"));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Changes the first process of one file and runs the cascade.
     *
     * @param componentColumns the columns of the component
     * @param file             the file to change
     * @param resource         the resource of the file
     * @param graph            the reference graph of the component
     * @param trial            the trial runner of the component
     * @return the row of the change
     * @throws IOException when a save or a read on the node fails
     */
    private List<String> runChange(List<String> componentColumns, String file, Resource resource,
            ReferenceGraph graph, CascadeTrial trial) throws IOException {
        List<String> row = new ArrayList<>(componentColumns);
        row.add(file);
        EObject process = NameChange.firstNamedObject(resource, Process.class);
        if (process == null) {
            row.addAll(Collections.nCopies(CascadeTrial.COLUMNS.size(), "-"));
            return row;
        }
        row.addAll(trial.run(resource, process, graph.widestTopReaching(resource)));
        return row;
    }

    /**
     * Loads every file of a component into its own IPFS resource, without vendor
     * extensions, which plain XMI cannot store.
     *
     * @param files           the files
     * @param ipfsResourceSet the resource set of the IPFS resources
     * @return the resource of each file, in the order of the files
     * @throws IOException when a file does not load
     */
    private Map<String, Resource> loadFiles(List<String> files, ResourceSet ipfsResourceSet) throws IOException {
        Map<String, Resource> resourceByFile = new LinkedHashMap<>();
        for (int index = 0; index < files.size(); index++) {
            Resource source = loader.load(datasetRoot.resolve(files.get(index)));
            VendorExtensions.strip(source);
            Definitions definitions = BpmnDefinitions.of(source);
            resourceByFile.put(files.get(index),
                    BpmnDefinitions.placeInNewResource(ipfsResourceSet, definitions, "file-" + (index + 1)));
        }
        return resourceByFile;
    }

    /**
     * Selects the links whose caller belongs to the component.
     *
     * @param resourceByFile the resources of the component
     * @param links          all call links
     * @return the links of the component
     */
    private static List<CallLink> linksOf(Map<String, Resource> resourceByFile, List<CallLink> links) {
        List<CallLink> selected = new ArrayList<>();
        for (CallLink link : links) {
            if (resourceByFile.containsKey(link.caller)) {
                selected.add(link);
            }
        }
        return selected;
    }

    /**
     * Sets the called element of every call activity of the links to the process
     * of the callee, which creates a reference between two resources.
     *
     * @param resourceByFile the resources of the component
     * @param links          the links of the component
     * @return the number of links that found both the call activity and the process
     */
    private static int applyLinks(Map<String, Resource> resourceByFile, List<CallLink> links) {
        int applied = 0;
        for (CallLink link : links) {
            CallActivity activity = findCallActivity(resourceByFile.get(link.caller), link.activity);
            Process process = findProcess(resourceByFile.get(link.callee), link.called);
            if (activity != null && process != null) {
                activity.setCalledElementRef(process);
                applied++;
            }
        }
        return applied;
    }

    /**
     * Finds a call activity by identifier.
     *
     * @param resource the resource of the caller
     * @param id       the identifier
     * @return the call activity, or null
     */
    private static CallActivity findCallActivity(Resource resource, String id) {
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            if (object instanceof CallActivity && id.equals(((CallActivity) object).getId())) {
                return (CallActivity) object;
            }
        }
        return null;
    }

    /**
     * Finds a process by identifier among the root elements.
     *
     * @param resource the resource of the callee
     * @param id       the identifier
     * @return the process, or null
     */
    private static Process findProcess(Resource resource, String id) {
        Definitions definitions = BpmnDefinitions.of(resource);
        for (RootElement element : definitions.getRootElements()) {
            if (element instanceof Process && id.equals(element.getId())) {
                return (Process) element;
            }
        }
        return null;
    }

    /**
     * Returns the column names of the table.
     *
     * @return the component columns followed by the trial columns
     */
    private static List<String> header() {
        List<String> columns = new ArrayList<>(CHANGE_COLUMNS);
        columns.addAll(CascadeTrial.COLUMNS);
        return columns;
    }
}
