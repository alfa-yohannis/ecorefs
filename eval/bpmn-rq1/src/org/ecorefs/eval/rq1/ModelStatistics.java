package org.ecorefs.eval.rq1;

import org.ecorefs.eval.bpmn.LocalProxyResolver;
import org.ecorefs.eval.bpmn.VendorExtensions;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Counts the properties of a loaded model that explain the results of the round
 * trip: its size, the objects without an identifier, the entries of vendor
 * extensions and the references to other files.
 */
final class ModelStatistics {

    private int objects;
    private int flowElements;
    private int missingIds;
    private int extensionEntries;
    private int externalProxies;

    /**
     * Prevents instances outside {@link #of(Resource)}.
     */
    private ModelStatistics() {
    }

    /**
     * Counts the properties of a loaded model.
     *
     * @param resource the loaded resource, after local proxies are resolved
     * @return the counts
     */
    static ModelStatistics of(Resource resource) {
        ModelStatistics statistics = new ModelStatistics();
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            statistics.count(iterator.next());
        }
        return statistics;
    }

    /**
     * Adds one object to the counts.
     *
     * @param object an object of the model
     */
    private void count(EObject object) {
        objects++;
        if (object instanceof FlowElement) {
            flowElements++;
        }
        EAttribute idAttribute = object.eClass().getEIDAttribute();
        if (idAttribute != null && !object.eIsSet(idAttribute)) {
            missingIds++;
        }
        for (EStructuralFeature feature : object.eClass().getEAllStructuralFeatures()) {
            countFeature(object, feature);
        }
    }

    /**
     * Counts the extension entries or the external proxies of one feature.
     *
     * @param object  the object
     * @param feature a feature of its class
     */
    private void countFeature(EObject object, EStructuralFeature feature) {
        if (feature.isDerived() || feature.isTransient() || !object.eIsSet(feature)) {
            return;
        }
        if (FeatureMapUtil.isFeatureMap(feature)) {
            countExtensionEntries((FeatureMap) object.eGet(feature));
        } else if (feature instanceof EReference && LocalProxyResolver.isStoredCrossReference((EReference) feature)) {
            countProxies(object.eGet(feature, false));
        }
    }

    /**
     * Counts the entries of a feature map that belong to a vendor namespace.
     *
     * @param featureMap the feature map
     */
    private void countExtensionEntries(FeatureMap featureMap) {
        for (FeatureMap.Entry entry : featureMap) {
            if (VendorExtensions.isVendorFeature(entry.getEStructuralFeature())) {
                extensionEntries++;
            }
        }
    }

    /**
     * Counts the proxies among the values of a cross-reference.
     *
     * @param value the value read without resolving
     */
    private void countProxies(Object value) {
        if (value instanceof InternalEList) {
            for (Object target : ((InternalEList<?>) value).basicList()) {
                countProxy(target);
            }
            return;
        }
        countProxy(value);
    }

    /**
     * Counts one target when it is a proxy.
     *
     * @param target the target of a reference
     */
    private void countProxy(Object target) {
        if (target instanceof EObject && ((EObject) target).eIsProxy()) {
            externalProxies++;
        }
    }

    /**
     * Returns the counts as columns of the result table.
     *
     * @return objects, flow elements, missing identifiers, extension entries and
     *         external proxies
     */
    String[] columns() {
        return new String[] { Integer.toString(objects), Integer.toString(flowElements), Integer.toString(missingIds),
                Integer.toString(extensionEntries), Integer.toString(externalProxies) };
    }
}
