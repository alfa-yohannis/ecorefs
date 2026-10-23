package org.ecorefs.eval.rq1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Gives every object without an identifier the name of its class and a number in
 * document order, such as {@code Task_3}, the scheme of the Eclipse BPMN2 Modeler.
 * The BPMN 2.0 resource of Eclipse would otherwise assign a random UUID during the
 * save, so two saves of the same model would write different bytes and receive
 * different CIDs. Identifiers already in the model are never reused.
 */
final class DocumentOrderIds {

    /**
     * Prevents instances; the class offers one static operation.
     */
    private DocumentOrderIds() {
    }

    /**
     * Assigns identifiers to every object of the resource that has an identifier
     * attribute without a value.
     *
     * @param resource the resource to complete
     * @return the number of assigned identifiers
     */
    static int assign(Resource resource) {
        Set<String> usedIds = collectIds(resource);
        Map<String, Integer> lastNumbers = new HashMap<>();
        int assigned = 0;
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            EAttribute idAttribute = object.eClass().getEIDAttribute();
            if (needsId(object, idAttribute)) {
                object.eSet(idAttribute, nextFreeId(object.eClass().getName(), usedIds, lastNumbers));
                assigned++;
            }
        }
        return assigned;
    }

    /**
     * Collects the identifiers that the resource already uses.
     *
     * @param resource the resource
     * @return the identifiers
     */
    private static Set<String> collectIds(Resource resource) {
        Set<String> ids = new HashSet<>();
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            EObject object = iterator.next();
            EAttribute idAttribute = object.eClass().getEIDAttribute();
            if (idAttribute != null && object.eIsSet(idAttribute)) {
                ids.add(String.valueOf(object.eGet(idAttribute)));
            }
        }
        return ids;
    }

    /**
     * Tells whether an object has a string identifier attribute without a value.
     *
     * @param object      the object
     * @param idAttribute the identifier attribute of its class, or null
     * @return true when the object needs an identifier
     */
    private static boolean needsId(EObject object, EAttribute idAttribute) {
        return idAttribute != null && idAttribute.getEAttributeType().getInstanceClass() == String.class
                && !object.eIsSet(idAttribute);
    }

    /**
     * Returns the next unused identifier for a class name and records it as used.
     *
     * @param className   the name of the class of the object
     * @param usedIds     the identifiers in use
     * @param lastNumbers the last number given per class name
     * @return the new identifier
     */
    private static String nextFreeId(String className, Set<String> usedIds, Map<String, Integer> lastNumbers) {
        int number = lastNumbers.getOrDefault(className, 0);
        String id;
        do {
            number++;
            id = className + "_" + number;
        } while (usedIds.contains(id));
        lastNumbers.put(className, number);
        usedIds.add(id);
        return id;
    }
}
