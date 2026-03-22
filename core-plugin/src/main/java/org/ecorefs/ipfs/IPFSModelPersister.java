package org.ecorefs.ipfs;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IPFSModelPersister {

    /**
     * Implements the "Git-like" recursive save for content-addressed models.
     * <p>
     * Behavior depends on the {@link ReferenceMode} option:
     * <ul>
     *   <li>{@link ReferenceMode#CID} (default) — saves the target, then cascades to all
     *       referencing resources in the ResourceSet, using a visited set to prevent infinite
     *       loops on circular dependencies.</li>
     *   <li>{@link ReferenceMode#IPNS} — saves only the target resource. IPNS pointers are
     *       mutable, so referencing resources do not need to be re-saved.</li>
     * </ul>
     */
    public static void cascadeSave(Resource targetResource, Map<?, ?> options) throws IOException {
        ReferenceMode mode = ReferenceMode.CID;
        if (options != null && options.containsKey(IPFSResourceOptions.REFERENCE_MODE)) {
            mode = (ReferenceMode) options.get(IPFSResourceOptions.REFERENCE_MODE);
        }

        if (mode == ReferenceMode.IPNS) {
            // IPNS mode: just save the target. The mutable pointer update is sufficient —
            // all resources referencing this one via ipns:// automatically see the new CID.
            targetResource.save(options);
        } else {
            // CID mode: full cascade with cycle detection
            cascadeSave(targetResource, options, new HashSet<>());
        }
    }

    private static void cascadeSave(Resource targetResource, Map<?, ?> options, Set<Resource> alreadySaved) throws IOException {
        // Prevent infinite loops on circular resource dependencies 
        if (alreadySaved.contains(targetResource)) {
            return;
        }
        
        // 1. IPFSResourceImpl.save() triggers the IPFS upload and dynamically mutates its own URI to the new CID
        targetResource.save(options);
        alreadySaved.add(targetResource);
        
        ResourceSet rs = targetResource.getResourceSet();
        if (rs != null) {
            // Find all inbound cross-references globally across the entire active ResourceSet
            Map<EObject, Collection<org.eclipse.emf.ecore.EStructuralFeature.Setting>> crossReferences = EcoreUtil.CrossReferencer.find(rs.getResources());
            
            Set<Resource> resourcesToSave = new HashSet<>();
            
            for (Map.Entry<EObject, Collection<org.eclipse.emf.ecore.EStructuralFeature.Setting>> entry : crossReferences.entrySet()) {
                EObject referencedObject = entry.getKey();
                
                // If the target of the reference lives inside the resource we just updated
                if (referencedObject.eResource() == targetResource) {
                    for (org.eclipse.emf.ecore.EStructuralFeature.Setting setting : entry.getValue()) {
                        Resource referencingResource = setting.getEObject().eResource();
                        
                        // Queue the referencing parent resource for an updated save
                        if (referencingResource != null && referencingResource != targetResource && !alreadySaved.contains(referencingResource)) {
                            resourcesToSave.add(referencingResource);
                        }
                    }
                }
            }
            
            // 2. EMF handles the textual update under the hood. When XMIResource serializes the 
            // queued parent resources, it polls the targetResource for its URI. Since the URI was 
            // mutated to the new IPFS CID in step 1, the new XMI href automatically reflects the upgrade.
            for (Resource parentResource : resourcesToSave) {
                cascadeSave(parentResource, options, alreadySaved);
            }
        }
    }
}
