package org.ecorefs.ipfs;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed graph of the references between the resources of one resource set.
 *
 * An edge runs from a resource to every other resource that the resource
 * references. The graph answers two questions of a cascade save: which resources
 * reach a changed resource, and in which order a save has to store the resources
 * so that every rewritten reference carries a current content identifier.
 */
public final class ResourceDependencyGraph {

    /** For each resource, the resources that the resource references. */
    private final Map<Resource, Set<Resource>> dependencies;

    /** For each resource, the resources that reference the resource. */
    private final Map<Resource, Set<Resource>> dependents;

    /**
     * Creates a graph from the two adjacency maps.
     *
     * @param dependencies outgoing edges of every resource
     * @param dependents   incoming edges of every resource
     */
    private ResourceDependencyGraph(Map<Resource, Set<Resource>> dependencies,
            Map<Resource, Set<Resource>> dependents) {
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    /**
     * Builds the graph from the references between the resources of a resource
     * set. References inside one resource are ignored, because such references
     * never change the content identifier of another resource.
     *
     * The traversal walks the resources directly instead of calling the cross
     * referencer of EMF, because the cross referencer reads every feature of
     * every object. Some generated metamodels leave a derived feature without an
     * implementation, and a read of such a feature then fails.
     *
     * @param resourceSet the resource set with the loaded resources
     * @return the dependency graph of the resource set
     */
    public static ResourceDependencyGraph of(ResourceSet resourceSet) {
        Map<Resource, Set<Resource>> dependencies = new HashMap<>();
        Map<Resource, Set<Resource>> dependents = new HashMap<>();
        for (Resource resource : new ArrayList<>(resourceSet.getResources())) {
            collectReferencesOfResource(resource, dependencies, dependents);
        }
        return new ResourceDependencyGraph(dependencies, dependents);
    }

    /**
     * Adds every reference of one resource into another resource to the graph.
     *
     * @param resource     the resource that holds the referencing objects
     * @param dependencies outgoing edges under construction
     * @param dependents   incoming edges under construction
     */
    private static void collectReferencesOfResource(Resource resource, Map<Resource, Set<Resource>> dependencies,
            Map<Resource, Set<Resource>> dependents) {
        Iterator<EObject> contents = resource.getAllContents();
        while (contents.hasNext()) {
            collectReferencesOfObject(resource, contents.next(), dependencies, dependents);
        }
    }

    /**
     * Adds every reference of one object into another resource to the graph.
     * Containment references and derived features are skipped, because a
     * containment reference stays inside one resource and a derived feature
     * carries no stored reference.
     *
     * @param resource     the resource that holds the object
     * @param object       the object with the references
     * @param dependencies outgoing edges under construction
     * @param dependents   incoming edges under construction
     */
    private static void collectReferencesOfObject(Resource resource, EObject object,
            Map<Resource, Set<Resource>> dependencies, Map<Resource, Set<Resource>> dependents) {
        for (EReference reference : object.eClass().getEAllReferences()) {
            if (reference.isContainment() || reference.isContainer() || reference.isDerived()
                    || reference.isVolatile()) {
                continue;
            }
            Object value = object.eGet(reference, true);
            if (value instanceof List) {
                for (Object target : (List<?>) value) {
                    addResourceEdge(resource, target, dependencies, dependents);
                }
            } else {
                addResourceEdge(resource, value, dependencies, dependents);
            }
        }
    }

    /**
     * Adds one edge when the target of a reference lives in another resource.
     *
     * @param resource     the resource that holds the referencing object
     * @param target       the referenced value
     * @param dependencies outgoing edges under construction
     * @param dependents   incoming edges under construction
     */
    private static void addResourceEdge(Resource resource, Object target,
            Map<Resource, Set<Resource>> dependencies, Map<Resource, Set<Resource>> dependents) {
        if (!(target instanceof EObject)) {
            return;
        }
        Resource referencedResource = ((EObject) target).eResource();
        if (referencedResource == null || referencedResource == resource) {
            return;
        }
        addEdge(dependencies, resource, referencedResource);
        addEdge(dependents, referencedResource, resource);
    }

    /**
     * Adds one entry to an adjacency map.
     *
     * @param adjacency the map that receives the entry
     * @param from      the resource at the start of the edge
     * @param to        the resource at the end of the edge
     */
    private static void addEdge(Map<Resource, Set<Resource>> adjacency, Resource from, Resource to) {
        Set<Resource> targets = adjacency.get(from);
        if (targets == null) {
            targets = new LinkedHashSet<>();
            adjacency.put(from, targets);
        }
        targets.add(to);
    }

    /**
     * Plans the order of a cascade save after a change to one resource. The list
     * starts with the changed resource and continues with every resource that
     * reaches the changed resource, so a resource appears after the resources
     * that the resource references. A reference cycle keeps one resource before a
     * resource of the same cycle, because no order satisfies every reference
     * inside a cycle.
     *
     * @param changedResource the resource with the new content
     * @return the resources to save, in save order
     */
    public List<Resource> planSaveOrder(Resource changedResource) {
        Set<Resource> affected = collectAffectedResources(changedResource);
        List<Resource> order = new ArrayList<>();
        Set<Resource> placed = new HashSet<>();
        Set<Resource> onPath = new HashSet<>();
        appendAfterDependencies(changedResource, affected, placed, onPath, order);
        for (Resource resource : affected) {
            appendAfterDependencies(resource, affected, placed, onPath, order);
        }
        return order;
    }

    /**
     * Collects the changed resource together with every resource that reaches the
     * changed resource through references.
     *
     * @param changedResource the resource with the new content
     * @return the resources that need a new save
     */
    private Set<Resource> collectAffectedResources(Resource changedResource) {
        Set<Resource> affected = new LinkedHashSet<>();
        List<Resource> pending = new ArrayList<>();
        affected.add(changedResource);
        pending.add(changedResource);

        while (!pending.isEmpty()) {
            Resource current = pending.remove(pending.size() - 1);
            Set<Resource> referencingResources = dependents.get(current);
            if (referencingResources == null) {
                continue;
            }
            for (Resource referencingResource : referencingResources) {
                if (affected.add(referencingResource)) {
                    pending.add(referencingResource);
                }
            }
        }
        return affected;
    }

    /**
     * Appends one resource to the save order, after every affected resource that
     * the resource references.
     *
     * @param resource the resource to append
     * @param affected the resources that need a new save
     * @param placed   the resources already in the order
     * @param onPath   the resources on the current search path, used to break cycles
     * @param order    the save order under construction
     */
    private void appendAfterDependencies(Resource resource, Set<Resource> affected, Set<Resource> placed,
            Set<Resource> onPath, List<Resource> order) {
        if (placed.contains(resource) || onPath.contains(resource)) {
            return;
        }
        onPath.add(resource);
        Set<Resource> referencedResources = dependencies.get(resource);
        if (referencedResources != null) {
            for (Resource referencedResource : referencedResources) {
                if (affected.contains(referencedResource)) {
                    appendAfterDependencies(referencedResource, affected, placed, onPath, order);
                }
            }
        }
        onPath.remove(resource);
        placed.add(resource);
        order.add(resource);
    }
}
