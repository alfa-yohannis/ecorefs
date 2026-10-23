package org.ecorefs.eval.baselines;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.Collection;

/**
 * Visits every element of a model and reads the identifier of every element. A
 * store that loads lazily, such as NeoEMF, fetches the elements only during the
 * visit, so the visit belongs to a full read. The result of the visit also
 * shows whether a store returned the complete model of the current iteration.
 */
public final class ModelTraversal {

    /** No instances; the class only offers static helpers. */
    private ModelTraversal() {
    }

    /**
     * Visits every element of a resource.
     *
     * @param resource the loaded resource
     * @return the number of elements and the sum of their identifiers
     */
    public static VisitSummary visitAll(Resource resource) {
        return visit(resource.getAllContents());
    }

    /**
     * Visits the given roots and every element they contain.
     *
     * @param roots the root objects of a model
     * @return the number of elements and the sum of their identifiers
     */
    public static VisitSummary visitAll(Collection<EObject> roots) {
        return visit(EcoreUtil.getAllContents(roots, false));
    }

    /**
     * Visits the elements of a tree iterator.
     *
     * @param contents the iterator over a model
     * @return the number of elements and the sum of their identifiers
     */
    private static VisitSummary visit(TreeIterator<? extends EObject> contents) {
        long count = 0;
        long identifierSum = 0;
        while (contents.hasNext()) {
            identifierSum += readIdentifier(contents.next());
            count++;
        }
        return new VisitSummary(count, identifierSum);
    }

    /**
     * Reads the identifier of one element.
     *
     * @param element the element
     * @return the identifier, or zero when the element has none
     */
    private static long readIdentifier(EObject element) {
        EStructuralFeature identifier = element.eClass().getEStructuralFeature("id");
        if (identifier == null) {
            return 0;
        }
        Object value = element.eGet(identifier);
        return value instanceof Integer ? (Integer) value : 0;
    }
}
