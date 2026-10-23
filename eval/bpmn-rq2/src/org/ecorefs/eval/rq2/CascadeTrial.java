package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.KuboNode;
import org.ecorefs.ipfs.IPFSModelPersister;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * One change and one cascade save on a group of published resources. The trial
 * renames one object, calls {@code IPFSModelPersister.cascadeSave} on its
 * resource, and checks the outcome three ways: the resources with a new CID against
 * the affected set of the independent reference graph (Proposition 1 of the paper),
 * the stored bytes of every resource for references to older CIDs (Proposition 2),
 * and a fresh load by CID that resolves every proxy through the node.
 */
final class CascadeTrial {

    /** Column suffixes of one trial in the result table. */
    static final List<String> COLUMNS = List.of("fan_in", "depth", "expected", "new_cids", "new_cids_match",
            "cascade_ms", "current_refs", "stale_refs", "unknown_refs", "stale_pairs", "load_resources",
            "load_expected", "load_stale", "load_unresolved", "load_errors", "change_visible");

    private final KuboNode node;
    private final List<Resource> resources;
    private final ReferenceGraph graph;
    private final CidHistory history;
    private final ExperimentResourceSets loadResourceSets;

    /**
     * Prepares trials on one group of resources.
     *
     * @param node             the node that stores the resources
     * @param resources        the published resources of the group
     * @param graph            the reference graph of the group
     * @param history          the CIDs that the resources have had
     * @param loadResourceSets the builder of the resource sets of the load check
     */
    CascadeTrial(KuboNode node, List<Resource> resources, ReferenceGraph graph, CidHistory history,
            ExperimentResourceSets loadResourceSets) {
        this.node = node;
        this.resources = resources;
        this.graph = graph;
        this.history = history;
        this.loadResourceSets = loadResourceSets;
    }

    /**
     * Runs one change.
     *
     * @param changedResource the resource of the changed object
     * @param changedObject   the object to rename
     * @param loadRoot        the resource from which the load check starts
     * @return the columns in the order of {@link #COLUMNS}; the visibility of the
     *         change is a dash when the load root does not reach the changed resource
     * @throws IOException when a save or a read on the node fails
     */
    List<String> run(Resource changedResource, EObject changedObject, Resource loadRoot) throws IOException {
        Map<Resource, URI> urisBefore = currentUris();
        String newName = NameChange.apply(changedObject);
        long start = System.nanoTime();
        IPFSModelPersister.cascadeSave(changedResource, null);
        long cascadeNanos = System.nanoTime() - start;
        history.record(resources);

        Set<Resource> withNewCid = changedSince(urisBefore);
        Set<Resource> expected = graph.affectedBy(changedResource);
        SnapshotCheck snapshot = SnapshotCheck.run(node, resources, history);
        LoadCheck load = LoadCheck.run(loadResourceSets, loadRoot, history, changedObject, newName);
        Set<Resource> reachable = graph.reachableFrom(loadRoot);
        String visible = reachable.contains(changedResource) ? (load.changeVisible ? "yes" : "no") : "-";
        return List.of(Integer.toString(graph.fanIn(changedResource)),
                Integer.toString(graph.chainDepth(changedResource)), Integer.toString(expected.size()),
                Integer.toString(withNewCid.size()), withNewCid.equals(expected) ? "yes" : "no", millis(cascadeNanos),
                Integer.toString(snapshot.currentReferences), Integer.toString(snapshot.staleReferences),
                Integer.toString(snapshot.unknownReferences), Integer.toString(snapshot.pairsWithStaleReferences),
                Integer.toString(load.loadedResources), Integer.toString(reachable.size()),
                Integer.toString(load.staleResources), Integer.toString(load.unresolvedProxies),
                Integer.toString(load.loadErrors), visible);
    }

    /**
     * Records the URI of every resource of the group.
     *
     * @return the URIs before the change
     */
    private Map<Resource, URI> currentUris() {
        Map<Resource, URI> uris = new IdentityHashMap<>();
        for (Resource resource : resources) {
            uris.put(resource, resource.getURI());
        }
        return uris;
    }

    /**
     * Collects the resources whose URI changed. A save in CID mode gives a new URI
     * whenever the bytes change, which they do for every resource of an acyclic
     * affected set, because a reference in it names a new CID. Inside a cycle, a
     * save can write the same bytes as before and keep its CID.
     *
     * @param urisBefore the URIs before the change
     * @return the re-saved resources
     */
    private Set<Resource> changedSince(Map<Resource, URI> urisBefore) {
        Set<Resource> changed = new HashSet<>();
        for (Resource resource : resources) {
            if (!resource.getURI().equals(urisBefore.get(resource))) {
                changed.add(resource);
            }
        }
        return changed;
    }

    /**
     * Formats a duration in milliseconds with three decimals.
     *
     * @param nanos the duration in nanoseconds
     * @return the formatted milliseconds
     */
    static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
    }
}
