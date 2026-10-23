package org.ecorefs.eval.rq2;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * The edit that starts every cascade: a new name for one object, as in the BPMN
 * case study of the paper, where the payment process gets a new name.
 */
final class NameChange {

    private static final String NAME = "name";
    private static final String SUFFIX = " (changed)";

    /**
     * Prevents instances; the class offers static operations only.
     */
    private NameChange() {
    }

    /**
     * Finds the first object of a resource, in document order, that has an
     * identifier and a string name attribute.
     *
     * @param resource the resource
     * @param type     the class the object must be an instance of
     * @return the object, or null when the resource holds none
     */
    static EObject firstNamedObject(Resource resource, Class<?> type) {
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            if (type.isInstance(object) && nameAttribute(object) != null && EcoreUtil.getID(object) != null) {
                return object;
            }
        }
        return null;
    }

    /**
     * Gives an object a new name.
     *
     * @param object the object
     * @return the new name
     */
    static String apply(EObject object) {
        String oldName = nameOf(object);
        String newName = (oldName == null ? "" : oldName) + SUFFIX;
        object.eSet(nameAttribute(object), newName);
        return newName;
    }

    /**
     * Reads the name of an object.
     *
     * @param object the object
     * @return the name, or null when the object has no name attribute or no name
     */
    static String nameOf(EObject object) {
        EAttribute attribute = nameAttribute(object);
        return attribute == null ? null : (String) object.eGet(attribute);
    }

    /**
     * Finds the string name attribute of an object.
     *
     * @param object the object
     * @return the attribute, or null
     */
    private static EAttribute nameAttribute(EObject object) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(NAME);
        if (feature instanceof EAttribute && ((EAttribute) feature).getEAttributeType().getInstanceClass() == String.class) {
            return (EAttribute) feature;
        }
        return null;
    }
}
