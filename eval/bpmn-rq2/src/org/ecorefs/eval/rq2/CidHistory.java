package org.ecorefs.eval.rq2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * Remembers every CID that a resource of the experiment has had, so a CID found in
 * stored bytes can be traced to its resource and classified as the current CID or
 * an older one.
 */
final class CidHistory {

    private final Map<String, Resource> resourceByCid = new HashMap<>();

    /**
     * Records the current CIDs of the resources.
     *
     * @param resources the resources after a save
     */
    void record(List<Resource> resources) {
        for (Resource resource : resources) {
            resourceByCid.put(currentCid(resource), resource);
        }
    }

    /**
     * Returns the resource that had a CID.
     *
     * @param cid the CID
     * @return the resource, or null when no resource of the experiment had the CID
     */
    Resource resourceOf(String cid) {
        return resourceByCid.get(cid);
    }

    /**
     * Returns the current CID of a resource.
     *
     * @param resource the resource
     * @return the authority of its {@code ipfs} URI
     */
    static String currentCid(Resource resource) {
        return resource.getURI().authority();
    }
}
