package org.ecorefs.ipfs;

import org.eclipse.emf.ecore.resource.Resource;

import java.util.Set;

/**
 * Keeps attributes that name another resource by its location in step with the
 * URI of that resource.
 *
 * EMF writes every reference with the current URI of its target, so a saved
 * reference always names the stored version of the target. Some metamodels also
 * keep the location of another resource in a plain attribute, for example the
 * location of an import in BPMN 2.0. EMF treats such an attribute as text and
 * never changes it. An updater writes the current URI of the named resource into
 * the attribute before a save, and it reports the named resources, so a cascade
 * save also reaches a resource that names another resource only through such an
 * attribute.
 *
 * Updaters take effect through the save option
 * {@link IPFSResourceOptions#LOCATION_UPDATERS}.
 */
public interface LocationAttributeUpdater {

    /**
     * Returns the resources of the same resource set that the location
     * attributes of a resource name.
     *
     * @param resource the resource with the location attributes
     * @return the named resources, never the resource itself
     */
    Set<Resource> findNamedResources(Resource resource);

    /**
     * Writes the current URI of the named resource into every location attribute
     * of a resource. An attribute whose resource is not loaded keeps its value.
     *
     * @param resource the resource about to be saved
     */
    void updateLocations(Resource resource);
}
