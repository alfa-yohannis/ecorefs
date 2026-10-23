package org.ecorefs.eval.baselines;

import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * Copies a model of plain dynamic objects into a NeoEMF resource whose package
 * has the same structure, matching classes and features by name. Every new
 * object is bound to the resource before it joins its container, so NeoEMF
 * writes it straight into the database. An unbound object would first go
 * through the transient store of NeoEMF, a set of Chronicle maps that NeoEMF
 * sizes for 65,536 entries; in a pilot run with 1,000 elements that detour
 * took about 2 s per copy instead of 35 to 80 ms. The copier handles
 * attributes and containment references, the only features of the benchmark
 * metamodel.
 */
public final class NeoEmfModelCopier {

    /** The package whose classes the copies get. */
    private final EPackage targetPackage;

    /**
     * Creates the copier.
     *
     * @param targetPackage the NeoEMF package whose classes the copies get
     */
    public NeoEmfModelCopier(EPackage targetPackage) {
        this.targetPackage = targetPackage;
    }

    /**
     * Copies the roots and everything they contain into the target resource,
     * breadth first.
     *
     * @param roots  the root objects of the source model
     * @param target the persistent NeoEMF resource that receives the copies
     */
    public void copyInto(Collection<EObject> roots, Resource target) {
        Resource.Internal database = (Resource.Internal) target;
        Deque<PendingCopy> pending = new ArrayDeque<>();
        for (EObject root : roots) {
            EObject rootCopy = createCopy(root, database);
            target.getContents().add(rootCopy);
            pending.add(new PendingCopy(root, rootCopy));
        }
        while (!pending.isEmpty()) {
            PendingCopy next = pending.poll();
            copyAttributes(next.source(), next.copy());
            copyContainedObjects(next.source(), next.copy(), database, pending);
        }
    }

    /**
     * Creates an empty NeoEMF object of the target class with the name of the
     * source class and binds the object to the database resource.
     *
     * @param source   the source object
     * @param database the resource that will hold the copy
     * @return the new, empty copy
     */
    private EObject createCopy(EObject source, Resource.Internal database) {
        EClass targetClass = (EClass) targetPackage.getEClassifier(source.eClass().getName());
        EObject copy = targetPackage.getEFactoryInstance().create(targetClass);
        PersistentEObject.from(copy).resource(database);
        return copy;
    }

    /**
     * Copies the set attribute values of one object.
     *
     * @param source the source object
     * @param copy   the bound copy
     */
    private void copyAttributes(EObject source, EObject copy) {
        for (EAttribute attribute : source.eClass().getEAllAttributes()) {
            if (source.eIsSet(attribute)) {
                copy.eSet(copy.eClass().getEStructuralFeature(attribute.getName()), source.eGet(attribute));
            }
        }
    }

    /**
     * Creates the copies of the objects that one object contains, puts them into
     * the copy of their container and queues them so their own features follow.
     *
     * @param source   the source object
     * @param copy     the bound copy
     * @param database the resource that holds the copies
     * @param pending  the queue of copies whose features are still missing
     */
    private void copyContainedObjects(EObject source, EObject copy, Resource.Internal database,
            Deque<PendingCopy> pending) {
        for (EReference containment : source.eClass().getEAllContainments()) {
            EReference targetContainment = (EReference) copy.eClass().getEStructuralFeature(containment.getName());
            for (EObject child : containedObjects(source, containment)) {
                EObject childCopy = createCopy(child, database);
                attach(copy, targetContainment, childCopy);
                pending.add(new PendingCopy(child, childCopy));
            }
        }
    }

    /**
     * Returns the objects that one containment reference holds.
     *
     * @param owner       the containing object
     * @param containment the containment reference
     * @return the contained objects, possibly none
     */
    @SuppressWarnings("unchecked")
    private static List<EObject> containedObjects(EObject owner, EReference containment) {
        if (containment.isMany()) {
            return (EList<EObject>) owner.eGet(containment);
        }
        EObject single = (EObject) owner.eGet(containment);
        return single == null ? List.of() : List.of(single);
    }

    /**
     * Puts a new copy into its container.
     *
     * @param container   the bound container
     * @param containment the containment reference of the container
     * @param child       the new copy
     */
    @SuppressWarnings("unchecked")
    private static void attach(EObject container, EReference containment, EObject child) {
        if (containment.isMany()) {
            ((EList<EObject>) container.eGet(containment)).add(child);
        } else {
            container.eSet(containment, child);
        }
    }

    /**
     * A source object and its bound copy whose features are still missing.
     *
     * @param source the source object
     * @param copy   the bound copy
     */
    private record PendingCopy(EObject source, EObject copy) {
    }
}
