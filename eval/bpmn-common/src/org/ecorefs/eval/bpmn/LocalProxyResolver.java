package org.ecorefs.eval.bpmn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Replaces the proxies that point into their own resource with the objects they
 * name. The BPMN 2.0 resource of Eclipse loads every reference written as a QName
 * as a proxy. Before the content moves to another resource, such a proxy must
 * become the object, or the new resource would refer back to the original file.
 * Proxies into other files stay as they are, so the experiment never loads a file
 * that a model names.
 */
public final class LocalProxyResolver {

    /**
     * Prevents instances; the class offers one static operation.
     */
    private LocalProxyResolver() {
    }

    /**
     * Resolves every proxy of the resource that points into the resource itself.
     *
     * @param resource the loaded resource
     */
    public static void resolveLocalProxies(Resource resource) {
        List<EObject> objects = new ArrayList<>();
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            objects.add(iterator.next());
        }
        for (EObject object : objects) {
            resolveReferencesOf(object, resource.getURI());
        }
    }

    /**
     * Resolves the local proxies in the cross-references of one object.
     *
     * @param object      the object whose references are checked
     * @param resourceUri the URI of the resource of the object
     */
    private static void resolveReferencesOf(EObject object, URI resourceUri) {
        for (EReference reference : object.eClass().getEAllReferences()) {
            if (isStoredCrossReference(reference)) {
                resolveReference(object, reference, resourceUri);
            }
        }
    }

    /**
     * Tells whether a reference holds stored cross-references that may be proxies.
     *
     * @param reference the reference
     * @return true for a non-containment reference that is stored and resolves proxies
     */
    public static boolean isStoredCrossReference(EReference reference) {
        return !reference.isContainment() && !reference.isContainer() && !reference.isDerived()
                && reference.isResolveProxies();
    }

    /**
     * Resolves the local proxies held by one reference of one object.
     *
     * @param object      the object
     * @param reference   the reference
     * @param resourceUri the URI of the resource of the object
     */
    private static void resolveReference(EObject object, EReference reference, URI resourceUri) {
        if (reference.isMany()) {
            resolveListEntries(object, reference, resourceUri);
            return;
        }
        if (isLocalProxy(object.eGet(reference, false), resourceUri)) {
            object.eGet(reference, true);
        }
    }

    /**
     * Resolves the local proxies in a many-valued reference. Reading an entry of
     * the list with {@code get} replaces a proxy by its object, while
     * {@code basicGet} reads without resolving.
     *
     * @param object      the object
     * @param reference   the many-valued reference
     * @param resourceUri the URI of the resource of the object
     */
    private static void resolveListEntries(EObject object, EReference reference, URI resourceUri) {
        Object value = object.eGet(reference, false);
        if (!(value instanceof InternalEList)) {
            return;
        }
        InternalEList<?> list = (InternalEList<?>) value;
        for (int index = 0; index < list.size(); index++) {
            if (isLocalProxy(list.basicGet(index), resourceUri)) {
                list.get(index);
            }
        }
    }

    /**
     * Tells whether a value is a proxy into the given resource.
     *
     * @param value       a value of a reference
     * @param resourceUri the URI of the resource
     * @return true for a proxy whose URI without fragment is the resource URI
     */
    private static boolean isLocalProxy(Object value, URI resourceUri) {
        if (!(value instanceof EObject) || !((EObject) value).eIsProxy()) {
            return false;
        }
        URI proxyUri = ((InternalEObject) value).eProxyURI();
        return proxyUri != null && proxyUri.trimFragment().equals(resourceUri);
    }
}
