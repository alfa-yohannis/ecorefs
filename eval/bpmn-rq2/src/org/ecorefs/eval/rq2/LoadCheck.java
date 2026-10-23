package org.ecorefs.eval.rq2;

import java.util.ArrayList;
import java.util.List;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.LocalProxyResolver;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Loads a published resource by its CID in a fresh resource set and resolves
 * every proxy, which fetches every resource it reaches from the node through the
 * standard proxy mechanism of EMF. The check counts the IPFS resources that were
 * loaded, those loaded under an older CID, and the IPFS proxies that stayed
 * unresolved. It also reads the attribute that the experiment changed, to show
 * that the loaded snapshot holds the change.
 */
final class LoadCheck {

    int loadedResources;
    int staleResources;
    int unresolvedProxies;
    int loadErrors;
    boolean changeVisible;

    /**
     * Runs the check.
     *
     * @param resourceSets  the builder of resource sets; it should allow no local folder
     * @param root          the published resource to start from
     * @param history       the CIDs that the resources have had
     * @param changed       the changed object on the saved side
     * @param expectedValue the new value of the changed name
     * @return the counts
     */
    static LoadCheck run(ExperimentResourceSets resourceSets, Resource root, CidHistory history, EObject changed,
            String expectedValue) {
        LoadCheck check = new LoadCheck();
        ResourceSet resourceSet = resourceSets.forIpfsXmi();
        try {
            resourceSet.getResource(URI.createURI("ipfs://" + CidHistory.currentCid(root)), true);
        } catch (RuntimeException e) {
            check.loadErrors++;
        }
        resolveAllProxies(resourceSet);
        for (Resource resource : new ArrayList<>(resourceSet.getResources())) {
            check.inspect(resource, history, changed, expectedValue);
        }
        return check;
    }

    /**
     * Resolves every proxy in the stored cross-references of the resource set,
     * including those of the resources that the resolution loads.
     * {@code EcoreUtil.resolveAll} cannot serve here: it also reads derived
     * features, and some derived features of the Eclipse BPMN 2.0 metamodel, such
     * as {@code Participant.incomingConversationLinks}, throw
     * {@code UnsupportedOperationException}.
     *
     * @param resourceSet the resource set
     */
    private static void resolveAllProxies(ResourceSet resourceSet) {
        for (int index = 0; index < resourceSet.getResources().size(); index++) {
            List<EObject> objects = new ArrayList<>();
            for (TreeIterator<EObject> iterator = resourceSet.getResources().get(index).getAllContents(); iterator
                    .hasNext();) {
                objects.add(iterator.next());
            }
            for (EObject object : objects) {
                resolveProxiesOf(object);
            }
        }
    }

    /**
     * Resolves the proxies in the stored cross-references of one object.
     *
     * @param object the object
     */
    private static void resolveProxiesOf(EObject object) {
        for (EReference reference : object.eClass().getEAllReferences()) {
            if (!LocalProxyResolver.isStoredCrossReference(reference)) {
                continue;
            }
            Object value = object.eGet(reference, false);
            if (value instanceof InternalEList) {
                InternalEList<?> list = (InternalEList<?>) value;
                for (int position = 0; position < list.size(); position++) {
                    if (list.basicGet(position) instanceof EObject && ((EObject) list.basicGet(position)).eIsProxy()) {
                        list.get(position);
                    }
                }
            } else if (value instanceof EObject && ((EObject) value).eIsProxy()) {
                object.eGet(reference, true);
            }
        }
    }

    /**
     * Counts one loaded resource. The changed object is found by its URI fragment
     * in the resource that carries the current CID of the changed resource,
     * because some exporters give two elements the same identifier.
     *
     * @param resource      a resource of the fresh resource set
     * @param history       the CIDs that the resources have had
     * @param changed       the changed object on the saved side
     * @param expectedValue the new value of the changed name
     */
    private void inspect(Resource resource, CidHistory history, EObject changed, String expectedValue) {
        if (!"ipfs".equals(resource.getURI().scheme()) || resource.getContents().isEmpty()) {
            return;
        }
        loadedResources++;
        loadErrors += resource.getErrors().size();
        Resource changedResource = changed.eResource();
        if (resource.getURI().authority().equals(CidHistory.currentCid(changedResource))) {
            EObject loadedChange = resource.getEObject(changedResource.getURIFragment(changed));
            changeVisible = loadedChange != null && expectedValue.equals(NameChange.nameOf(loadedChange));
        }
        Resource original = history.resourceOf(resource.getURI().authority());
        if (original == null || !resource.getURI().authority().equals(CidHistory.currentCid(original))) {
            staleResources++;
        }
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            countUnresolvedProxies(object);

        }
    }

    /**
     * Counts the IPFS proxies of one object that resolution left in place. A
     * reference that does not resolve proxies, such as a sequence flow across two
     * pools, keeps its proxy after every load and counts here too.
     *
     * @param object a loaded object
     */
    private void countUnresolvedProxies(EObject object) {
        for (EReference reference : object.eClass().getEAllReferences()) {
            if (!ReferenceGraph.isSerializedCrossReference(reference)) {
                continue;
            }
            Object value = object.eGet(reference, false);
            List<?> targets = value instanceof InternalEList ? ((InternalEList<?>) value).basicList()
                    : (value == null ? List.of() : List.of(value));
            for (Object target : targets) {
                if (target instanceof EObject && ((EObject) target).eIsProxy()
                        && "ipfs".equals(((InternalEObject) target).eProxyURI().scheme())) {
                    unresolvedProxies++;
                }
            }
        }
    }

    /**
     * Tells whether the load reached a consistent snapshot with the change.
     *
     * @param expectedResources the number of resources the start resource reaches
     * @return true when every reached resource loaded under its current CID
     */
    boolean passed(int expectedResources) {
        return loadedResources == expectedResources && staleResources == 0 && unresolvedProxies == 0;
    }
}
