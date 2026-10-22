package org.ecorefs.ipfs;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps the import locations of a metamodel that imports other models by
 * namespace in step with the URIs of the imported resources. BPMN 2.0 is such a
 * metamodel: an import names the namespace of the imported definitions and the
 * location to read them from, and a reference such as
 * {@code order:Task_ReserveItem} reaches the imported element through the
 * namespace of its prefix.
 *
 * For every import, the updater looks for the one other resource of the
 * resource set that declares the namespace of the import as its target
 * namespace, and writes the URI of that resource into the location of the
 * import. After a save to IPFS, the location therefore names the same version as
 * the references: an {@code ipfs://} CID in CID mode and an {@code ipns://} name
 * in IPNS mode. An import keeps its location when no loaded resource declares its
 * namespace, or when more than one does.
 *
 * The updater reads the metamodel only through the three attributes given to the
 * constructor, so the core plug-in needs no dependency on BPMN:
 *
 * <pre>{@code
 * LocationAttributeUpdater bpmnImports = new NamespaceImportLocations(
 *         Bpmn2Package.Literals.IMPORT__NAMESPACE,
 *         Bpmn2Package.Literals.IMPORT__LOCATION,
 *         Bpmn2Package.Literals.DEFINITIONS__TARGET_NAMESPACE);
 * }</pre>
 */
public final class NamespaceImportLocations implements LocationAttributeUpdater {

    /** Attribute of an import that holds the namespace of the imported model. */
    private final EAttribute importNamespace;

    /** Attribute of an import that holds the location of the imported model. */
    private final EAttribute importLocation;

    /** Attribute of the object that contains the imports and declares the namespace of its model. */
    private final EAttribute targetNamespace;

    /**
     * Creates an updater for one metamodel. The class that contains the import
     * location is the import class, and the class that contains the target
     * namespace is the class whose objects hold the imports.
     *
     * @param importNamespace attribute of an import that holds the imported namespace
     * @param importLocation  attribute of an import that holds the location
     * @param targetNamespace attribute that declares the namespace of a model
     */
    public NamespaceImportLocations(EAttribute importNamespace, EAttribute importLocation,
            EAttribute targetNamespace) {
        this.importNamespace = Objects.requireNonNull(importNamespace, "importNamespace");
        this.importLocation = Objects.requireNonNull(importLocation, "importLocation");
        this.targetNamespace = Objects.requireNonNull(targetNamespace, "targetNamespace");
    }

    /**
     * Returns the resources that the imports of a resource name, found through
     * the namespaces that the resources of the resource set declare.
     *
     * @param resource the resource with the imports
     * @return the imported resources, never the resource itself
     */
    @Override
    public Set<Resource> findNamedResources(Resource resource) {
        Map<Object, List<Resource>> declaringResources = indexResourcesByNamespace(resource.getResourceSet());
        Set<Resource> importedResources = new LinkedHashSet<>();
        for (EObject anImport : findImports(resource)) {
            Resource importedResource = findImportedResource(resource, anImport, declaringResources);
            if (importedResource != null) {
                importedResources.add(importedResource);
            }
        }
        return importedResources;
    }

    /**
     * Writes the URI of the imported resource into the location of every import
     * of a resource whose imported resource is loaded.
     *
     * @param resource the resource about to be saved
     */
    @Override
    public void updateLocations(Resource resource) {
        Map<Object, List<Resource>> declaringResources = indexResourcesByNamespace(resource.getResourceSet());
        for (EObject anImport : findImports(resource)) {
            Resource importedResource = findImportedResource(resource, anImport, declaringResources);
            if (importedResource != null) {
                anImport.eSet(importLocation, importedResource.getURI().toString());
            }
        }
    }

    /**
     * Groups the resources of a resource set by the namespace that each resource
     * declares. A resource without a declaring object, such as a file resource
     * whose content moved into an IPFS resource, appears under no namespace.
     *
     * @param resourceSet the resource set, or null for a resource outside a set
     * @return for every declared namespace, the resources that declare it
     */
    private Map<Object, List<Resource>> indexResourcesByNamespace(ResourceSet resourceSet) {
        Map<Object, List<Resource>> declaringResources = new HashMap<>();
        if (resourceSet == null) {
            return declaringResources;
        }
        for (Resource candidate : resourceSet.getResources()) {
            Object namespace = findDeclaredNamespace(candidate);
            if (namespace != null) {
                addToIndex(declaringResources, namespace, candidate);
            }
        }
        return declaringResources;
    }

    /**
     * Adds one resource to the list of the resources that declare a namespace.
     *
     * @param declaringResources the index under construction
     * @param namespace          the declared namespace
     * @param resource           the resource that declares the namespace
     */
    private static void addToIndex(Map<Object, List<Resource>> declaringResources, Object namespace,
            Resource resource) {
        List<Resource> resources = declaringResources.get(namespace);
        if (resources == null) {
            resources = new ArrayList<>();
            declaringResources.put(namespace, resources);
        }
        resources.add(resource);
    }

    /**
     * Returns the resource that an import names: the one resource other than the
     * importing resource that declares the namespace of the import.
     *
     * @param importingResource  the resource that holds the import
     * @param anImport           the import
     * @param declaringResources the resources of the resource set, by declared namespace
     * @return the imported resource, or null when no other resource or more than
     *         one other resource declares the namespace
     */
    private Resource findImportedResource(Resource importingResource, EObject anImport,
            Map<Object, List<Resource>> declaringResources) {
        List<Resource> candidates = declaringResources.get(anImport.eGet(importNamespace));
        if (candidates == null) {
            return null;
        }
        List<Resource> otherResources = new ArrayList<>(candidates);
        otherResources.remove(importingResource);
        return otherResources.size() == 1 ? otherResources.get(0) : null;
    }

    /**
     * Returns the imports of a resource: the objects of the import class directly
     * inside the object that declares the namespace of the resource.
     *
     * @param resource the resource with the imports
     * @return the imports, possibly empty
     */
    private List<EObject> findImports(Resource resource) {
        List<EObject> imports = new ArrayList<>();
        EObject declaringObject = findDeclaringObject(resource);
        if (declaringObject == null) {
            return imports;
        }
        EClass importClass = importLocation.getEContainingClass();
        for (EObject child : declaringObject.eContents()) {
            if (importClass.isInstance(child)) {
                imports.add(child);
            }
        }
        return imports;
    }

    /**
     * Returns the namespace that a resource declares.
     *
     * @param resource the resource
     * @return the target namespace of the declaring object, or null when the
     *         resource has no declaring object or the object declares no namespace
     */
    private Object findDeclaredNamespace(Resource resource) {
        EObject declaringObject = findDeclaringObject(resource);
        return declaringObject == null ? null : declaringObject.eGet(targetNamespace);
    }

    /**
     * Returns the object of a resource that declares the namespace of its model:
     * a root object of the declaring class, or a direct child of a root object.
     * The second case covers BPMN 2.0, whose definitions sit inside a document
     * root when a model is read from BPMN XML.
     *
     * @param resource the resource
     * @return the declaring object, or null when the resource holds none
     */
    private EObject findDeclaringObject(Resource resource) {
        EClass declaringClass = targetNamespace.getEContainingClass();
        for (EObject root : resource.getContents()) {
            if (declaringClass.isInstance(root)) {
                return root;
            }
            EObject child = findChildOfClass(root, declaringClass);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    /**
     * Returns the first direct child of an object that belongs to a class.
     *
     * @param parent the object whose children to search
     * @param eClass the class of the wanted child
     * @return the child, or null when no direct child belongs to the class
     */
    private static EObject findChildOfClass(EObject parent, EClass eClass) {
        for (EObject child : parent.eContents()) {
            if (eClass.isInstance(child)) {
                return child;
            }
        }
        return null;
    }
}
