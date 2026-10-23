package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ecorefs.ipfs.IPFSResourceImpl;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * The references between a fixed list of resources, found by a scan of the
 * stored cross-references of every object. The scan is independent of the
 * dependency graph of the core plugin, so the experiment can check the cascade
 * of the core plugin against it. An edge from A to B means that an object of A
 * refers to an object of B, so a new CID of B needs a new save of A.
 */
final class ReferenceGraph {

    private final List<Resource> resources;
    private final Map<Resource, Set<Resource>> referenced = new LinkedHashMap<>();
    private final Map<Resource, Set<Resource>> referencing = new LinkedHashMap<>();

    /**
     * Creates an empty graph over the resources.
     *
     * @param resources the resources of the graph
     */
    private ReferenceGraph(List<Resource> resources) {
        this.resources = new ArrayList<>(resources);
        for (Resource resource : resources) {
            referenced.put(resource, new LinkedHashSet<>());
            referencing.put(resource, new LinkedHashSet<>());
        }
    }

    /**
     * Scans the resources and builds the graph.
     *
     * @param resources the resources, all in memory
     * @return the graph
     */
    static ReferenceGraph of(List<Resource> resources) {
        ReferenceGraph graph = new ReferenceGraph(resources);
        for (Resource resource : resources) {
            graph.scan(resource);
        }
        return graph;
    }

    /**
     * Adds the edges of every object of one resource.
     *
     * @param resource the resource
     */
    private void scan(Resource resource) {
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            for (EReference reference : object.eClass().getEAllReferences()) {
                if (isSerializedCrossReference(reference)) {
                    addEdges(resource, object.eGet(reference, false));
                }
            }
        }
    }

    /**
     * Tells whether XMI writes a reference as a cross-reference. References that do
     * not resolve proxies count too: a sequence flow that crosses two pools, which
     * BPMN forbids but some models of the dataset draw, refers to a node of another
     * process through {@code targetRef}, and the XMI of the process names the CID
     * of the other resource.
     *
     * @param reference the reference
     * @return true for a stored non-containment reference
     */
    static boolean isSerializedCrossReference(EReference reference) {
        return !reference.isContainment() && !reference.isContainer() && !reference.isDerived()
                && !reference.isTransient();
    }

    /**
     * Adds an edge for every target of a reference that lies in another resource
     * of the graph. Proxies stay out, because the scan never loads anything.
     *
     * @param resource the resource of the referencing object
     * @param value    the value of the reference, read without resolving
     */
    private void addEdges(Resource resource, Object value) {
        if (value == null) {
            return;
        }
        List<?> targets = value instanceof InternalEList ? ((InternalEList<?>) value).basicList() : List.of(value);
        for (Object target : targets) {
            if (target instanceof EObject && !((EObject) target).eIsProxy()) {
                Resource targetResource = ((EObject) target).eResource();
                if (targetResource != resource && referenced.containsKey(targetResource)) {
                    referenced.get(resource).add(targetResource);
                    referencing.get(targetResource).add(resource);
                }
            }
        }
    }

    /**
     * Returns the number of edges.
     *
     * @return the number of ordered pairs of resources with a reference
     */
    int edgeCount() {
        int edges = 0;
        for (Set<Resource> targets : referenced.values()) {
            edges += targets.size();
        }
        return edges;
    }

    /**
     * Returns the resources that refer directly to a resource.
     *
     * @param resource the resource
     * @return the number of direct referencing resources
     */
    int fanIn(Resource resource) {
        return referencing.get(resource).size();
    }

    /**
     * Collects the affected set of a change: the changed resource and every
     * resource that reaches it through references.
     *
     * @param changed the changed resource
     * @return the affected resources
     */
    Set<Resource> affectedBy(Resource changed) {
        Set<Resource> affected = new LinkedHashSet<>();
        List<Resource> pending = new ArrayList<>();
        affected.add(changed);
        pending.add(changed);
        while (!pending.isEmpty()) {
            Resource current = pending.remove(pending.size() - 1);
            for (Resource source : referencing.get(current)) {
                if (affected.add(source)) {
                    pending.add(source);
                }
            }
        }
        return affected;
    }

    /**
     * Collects the resources that a resource reaches through references, the
     * resource itself included. A load of the resource by CID fetches exactly
     * these resources.
     *
     * @param start the resource to start from
     * @return the reached resources
     */
    Set<Resource> reachableFrom(Resource start) {
        Set<Resource> reached = new LinkedHashSet<>();
        List<Resource> pending = new ArrayList<>();
        reached.add(start);
        pending.add(start);
        while (!pending.isEmpty()) {
            Resource current = pending.remove(pending.size() - 1);
            for (Resource target : referenced.get(current)) {
                if (reached.add(target)) {
                    pending.add(target);
                }
            }
        }
        return reached;
    }

    /**
     * Returns the top resource from which a load reaches the most resources,
     * among the top resources that reach the given one. A load check from there
     * covers the given resource and everything around it.
     *
     * @param resource the resource the load must reach
     * @return the top resource, or the resource itself when no top resource reaches it
     */
    Resource widestTopReaching(Resource resource) {
        Resource best = resource;
        int bestSize = reachableFrom(resource).size();
        for (Resource top : topResources()) {
            Set<Resource> reached = reachableFrom(top);
            if (reached.contains(resource) && reached.size() > bestSize) {
                best = top;
                bestSize = reached.size();
            }
        }
        return best;
    }

    /**
     * Publishes every resource for the first time, in publication order.
     *
     * @return the duration of all saves in nanoseconds
     * @throws IOException when a save fails
     */
    long publishAll() throws IOException {
        long start = System.nanoTime();
        for (Resource resource : publicationOrder()) {
            ((IPFSResourceImpl) resource).saveIPFS(null);
        }
        return System.nanoTime() - start;
    }

    /**
     * Returns the length of the longest chain of references that ends at a
     * resource, counted in edges. Inside a cycle the walk stops at a resource that
     * it has already entered.
     *
     * @param resource the resource at the end of the chains
     * @return the chain depth
     */
    int chainDepth(Resource resource) {
        return longestChainAbove(resource, new HashSet<>(), new HashMap<>());
    }

    /**
     * Computes the longest chain above a resource.
     *
     * @param resource the resource
     * @param entered  the resources on the current walk
     * @param known    the depths computed so far
     * @return the length of the longest chain in edges
     */
    private int longestChainAbove(Resource resource, Set<Resource> entered, Map<Resource, Integer> known) {
        Integer depth = known.get(resource);
        if (depth != null) {
            return depth;
        }
        entered.add(resource);
        int longest = 0;
        for (Resource source : referencing.get(resource)) {
            if (!entered.contains(source)) {
                longest = Math.max(longest, 1 + longestChainAbove(source, entered, known));
            }
        }
        entered.remove(resource);
        known.put(resource, longest);
        return longest;
    }

    /**
     * Tells whether the references form a cycle.
     *
     * @return true when some resource reaches itself
     */
    boolean hasCycle() {
        for (Resource resource : resources) {
            if (reaches(resource, resource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a resource reaches a target through one or more references.
     *
     * @param start  the resource to start from
     * @param target the resource to reach
     * @return true when a path of at least one edge exists
     */
    private boolean reaches(Resource start, Resource target) {
        Set<Resource> seen = new HashSet<>();
        List<Resource> pending = new ArrayList<>(referenced.get(start));
        while (!pending.isEmpty()) {
            Resource current = pending.remove(pending.size() - 1);
            if (current == target) {
                return true;
            }
            if (seen.add(current)) {
                pending.addAll(referenced.get(current));
            }
        }
        return false;
    }

    /**
     * Returns an order for the first publication in which every resource comes
     * after the resources it refers to. A cycle leaves one reference behind.
     *
     * @return the resources in publication order
     */
    List<Resource> publicationOrder() {
        List<Resource> order = new ArrayList<>();
        Set<Resource> placed = new HashSet<>();
        Set<Resource> entered = new HashSet<>();
        for (Resource resource : resources) {
            appendAfterTargets(resource, placed, entered, order);
        }
        return order;
    }

    /**
     * Appends a resource after the resources it refers to.
     *
     * @param resource the resource
     * @param placed   the resources already in the order
     * @param entered  the resources on the current walk
     * @param order    the order under construction
     */
    private void appendAfterTargets(Resource resource, Set<Resource> placed, Set<Resource> entered,
            List<Resource> order) {
        if (placed.contains(resource) || entered.contains(resource)) {
            return;
        }
        entered.add(resource);
        for (Resource target : referenced.get(resource)) {
            appendAfterTargets(target, placed, entered, order);
        }
        entered.remove(resource);
        placed.add(resource);
        order.add(resource);
    }

    /**
     * Returns the resources that no other resource refers to.
     *
     * @return the top resources
     */
    List<Resource> topResources() {
        List<Resource> tops = new ArrayList<>();
        for (Resource resource : resources) {
            if (referencing.get(resource).isEmpty()) {
                tops.add(resource);
            }
        }
        return tops;
    }
}
