package org.ecorefs.eval.bpmn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

/**
 * Removes the vendor extensions of a BPMN model: the attributes and elements of
 * namespaces other than BPMN 2.0, its diagram interchange and the XML type package
 * of EMF. Plain XMI, the serialization of {@code IPFSResourceImpl}, cannot store
 * them, as the RQ1 round trip shows, while they carry no reference between the
 * resources of a split model.
 */
public final class VendorExtensions {

    /**
     * Namespaces whose content stays: the XML namespaces of BPMN 2.0 and its
     * diagram interchange, the package URIs under which the Eclipse metamodel
     * registers the same packages, and the XML type package of EMF, which holds
     * text and comments. Attributes of the XML namespace, such as {@code xml:lang},
     * land in the same attribute map as vendor attributes, and plain XMI cannot
     * load them either, so they go too.
     */
    public static final Set<String> STANDARD_NAMESPACES = new HashSet<>(Arrays.asList(
            "http://www.omg.org/spec/BPMN/20100524/MODEL",
            "http://www.omg.org/spec/BPMN/20100524/DI",
            "http://www.omg.org/spec/DD/20100524/DC",
            "http://www.omg.org/spec/DD/20100524/DI",
            "http://www.omg.org/spec/BPMN/20100524/MODEL-XMI",
            "http://www.omg.org/spec/BPMN/20100524/DI-XMI",
            "http://www.omg.org/spec/DD/20100524/DC-XMI",
            "http://www.omg.org/spec/DD/20100524/DI-XMI",
            "http://www.eclipse.org/emf/2003/XMLType"));

    /**
     * Tells whether a feature of a feature map entry belongs to a vendor namespace.
     *
     * @param entryFeature the feature of the entry
     * @return true when the package of the feature is not a standard one
     */
    public static boolean isVendorFeature(EStructuralFeature entryFeature) {
        String namespace = entryFeature.getEContainingClass().getEPackage().getNsURI();
        return !STANDARD_NAMESPACES.contains(namespace);
    }

    /**
     * Prevents instances; the class offers one static operation.
     */
    private VendorExtensions() {
    }

    /**
     * Removes every vendor extension of the resource.
     *
     * @param resource the loaded BPMN resource
     * @return the number of removed extension values and feature map entries
     */
    public static int strip(Resource resource) {
        List<EObject> objects = new ArrayList<>();
        for (TreeIterator<EObject> iterator = resource.getAllContents(); iterator.hasNext();) {
            objects.add(iterator.next());
        }
        int removed = 0;
        for (EObject object : objects) {
            removed += stripObject(object);
        }
        return removed;
    }

    /**
     * Removes the extension values and the vendor entries of one object.
     *
     * @param object an object of the model
     * @return the number of removed items
     */
    private static int stripObject(EObject object) {
        int removed = 0;
        if (object instanceof BaseElement) {
            BaseElement element = (BaseElement) object;
            removed += element.getExtensionValues().size() + element.getExtensionDefinitions().size();
            element.getExtensionValues().clear();
            element.getExtensionDefinitions().clear();
        }
        for (EStructuralFeature feature : object.eClass().getEAllStructuralFeatures()) {
            if (FeatureMapUtil.isFeatureMap(feature) && !feature.isDerived() && object.eIsSet(feature)) {
                removed += stripFeatureMap((FeatureMap) object.eGet(feature));
            }
        }
        return removed;
    }

    /**
     * Removes the entries of vendor namespaces from a feature map.
     *
     * @param featureMap the feature map
     * @return the number of removed entries
     */
    private static int stripFeatureMap(FeatureMap featureMap) {
        int removed = 0;
        for (int index = featureMap.size() - 1; index >= 0; index--) {
            if (isVendorFeature(featureMap.getEStructuralFeature(index))) {
                featureMap.remove(index);
                removed++;
            }
        }
        return removed;
    }
}
