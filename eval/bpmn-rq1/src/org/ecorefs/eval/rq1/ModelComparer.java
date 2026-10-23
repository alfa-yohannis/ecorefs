package org.ecorefs.eval.rq1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ecorefs.eval.bpmn.Failures;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Compares the model that was saved with the model that was loaded back. The
 * comparison walks both containment trees in parallel and compares the class, the
 * set state and the value of every stored feature, including the entries of
 * feature maps, which hold vendor extensions in BPMN. Cross-references are
 * compared after the walk: a reference inside the model must lead to the matching
 * object, and a reference to another file must name the same file name and
 * fragment. Proxies are never resolved, so the comparison loads no other file.
 * Derived and transient features are skipped, because no serialization stores them.
 *
 * <p>The first difference is reported with a category in front:
 * {@code content} for missing or extra objects, {@code extension-attribute} for
 * attributes of vendor namespaces, {@code feature-map} for other feature map
 * entries, {@code value} for attribute values, {@code dangling-reference} for a
 * reference whose saved target is an identifier that exists nowhere in the file,
 * {@code duplicate-id} for a reference to an identifier that the file declares
 * twice, and {@code reference} for other references.
 */
final class ModelComparer {

    private static final int MAXIMUM_DIFFERENCE_LENGTH = 240;
    private static final String EXTENSION_ATTRIBUTES = "anyAttribute";

    private final URI leftBase;
    private final URI rightBase;
    private final URI sourceUri;
    private final Map<EObject, EObject> matches = new IdentityHashMap<>();
    private final List<PendingReference> pendingReferences = new ArrayList<>();
    private String difference;

    /**
     * Creates a comparer for one pair of resources.
     *
     * @param leftBase  the URI of the saved resource, against which its references are read
     * @param rightBase the URI of the loaded resource
     * @param sourceUri the URI of the original file, which dangling proxies name
     */
    private ModelComparer(URI leftBase, URI rightBase, URI sourceUri) {
        this.leftBase = leftBase;
        this.rightBase = rightBase;
        this.sourceUri = sourceUri;
    }

    /**
     * Compares two resources.
     *
     * @param left      the saved resource
     * @param right     the loaded resource
     * @param sourceUri the URI of the original file of the model
     * @return null when the models are equal, otherwise the category and the first difference
     */
    static String findDifference(Resource left, Resource right, URI sourceUri) {
        ModelComparer comparer = new ModelComparer(left.getURI(), right.getURI(), sourceUri);
        return comparer.compareResources(left, right) ? null : comparer.difference;
    }

    /**
     * Compares the contents and then the pending cross-references.
     *
     * @param left  the saved resource
     * @param right the loaded resource
     * @return true when the models are equal
     */
    private boolean compareResources(Resource left, Resource right) {
        if (!compareObjectLists(left.getContents(), right.getContents(), "")) {
            return false;
        }
        for (PendingReference pending : pendingReferences) {
            if (!comparePendingReference(pending)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two lists of contained objects position by position.
     *
     * @param left  the objects on the saved side
     * @param right the objects on the loaded side
     * @param path  the path of the list, for the report
     * @return true when the lists are equal
     */
    private boolean compareObjectLists(List<?> left, List<?> right, String path) {
        if (left.size() != right.size()) {
            return fail("content", path + ": " + left.size() + " objects, loaded " + right.size());
        }
        for (int index = 0; index < left.size(); index++) {
            if (!compareObjects((EObject) left.get(index), (EObject) right.get(index), path + "[" + index + "]")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two contained objects and records them as a match.
     *
     * @param left  the object on the saved side
     * @param right the object on the loaded side
     * @param path  the path of the object, for the report
     * @return true when the objects and their contents are equal
     */
    private boolean compareObjects(EObject left, EObject right, String path) {
        String objectPath = path + "/" + left.eClass().getName() + idSuffix(left);
        if (!classKey(left.eClass()).equals(classKey(right.eClass()))) {
            return fail("content", objectPath + ": loaded as " + classKey(right.eClass()));
        }
        matches.put(left, right);
        for (EStructuralFeature feature : left.eClass().getEAllStructuralFeatures()) {
            if (isStored(feature) && !compareFeature(left, right, feature, objectPath)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares one stored feature of two matched objects.
     *
     * @param left    the object on the saved side
     * @param right   the object on the loaded side
     * @param feature the feature of the saved object's class
     * @param path    the path of the object, for the report
     * @return true when the set state and the values are equal
     */
    private boolean compareFeature(EObject left, EObject right, EStructuralFeature feature, String path) {
        String featurePath = path + "." + feature.getName();
        EStructuralFeature rightFeature = matchingFeature(right.eClass(), feature);
        if (rightFeature == null) {
            return fail("content", featurePath + ": feature missing after the load");
        }
        boolean leftSet = left.eIsSet(feature);
        if (leftSet != right.eIsSet(rightFeature)) {
            return fail(categoryOf(left, feature), featurePath + ": " + (leftSet ? "set, loaded unset" : "unset, loaded set"));
        }
        if (!leftSet) {
            return true;
        }
        if (FeatureMapUtil.isFeatureMap(feature)) {
            return compareFeatureMaps((FeatureMap) left.eGet(feature), (FeatureMap) right.eGet(rightFeature),
                    featurePath, featureMapCategory(feature));
        }
        if (feature instanceof EAttribute) {
            return compareValues(left.eGet(feature), right.eGet(rightFeature), featurePath);
        }
        EReference reference = (EReference) feature;
        if (reference.isContainment()) {
            return compareObjectLists(asList(left.eGet(feature)), asList(right.eGet(rightFeature)), featurePath);
        }
        pendingReferences.add(new PendingReference(storedValues(left.eGet(feature, false)),
                storedValues(right.eGet(rightFeature, false)), featurePath));
        return true;
    }

    /**
     * Compares two feature maps entry by entry. An entry holds an element or an
     * attribute of a vendor extension, or text and comments of mixed content.
     *
     * @param left     the feature map on the saved side
     * @param right    the feature map on the loaded side
     * @param path     the path of the feature map, for the report
     * @param category the category of a difference in the feature map
     * @return true when the entries are equal
     */
    private boolean compareFeatureMaps(FeatureMap left, FeatureMap right, String path, String category) {
        if (left.size() != right.size()) {
            return fail(category, path + ": " + left.size() + " entries, loaded " + right.size());
        }
        for (int index = 0; index < left.size(); index++) {
            if (!compareFeatureMapEntries(left.get(index), right.get(index), path + "[" + index + "]", category)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two entries of feature maps.
     *
     * @param left     the entry on the saved side
     * @param right    the entry on the loaded side
     * @param path     the path of the entry, for the report
     * @param category the category of a difference in the feature map
     * @return true when the entries name the same feature and hold equal values
     */
    private boolean compareFeatureMapEntries(FeatureMap.Entry left, FeatureMap.Entry right, String path,
            String category) {
        EStructuralFeature leftFeature = left.getEStructuralFeature();
        String entryPath = path + "." + ExtendedMetaData.INSTANCE.getName(leftFeature);
        if (!featureKey(leftFeature).equals(featureKey(right.getEStructuralFeature()))) {
            return fail(category, entryPath + ": loaded as " + featureKey(right.getEStructuralFeature()));
        }
        if (leftFeature instanceof EAttribute) {
            return compareValues(left.getValue(), right.getValue(), entryPath);
        }
        if (((EReference) leftFeature).isContainment()) {
            return compareObjects((EObject) left.getValue(), (EObject) right.getValue(), entryPath);
        }
        pendingReferences.add(new PendingReference(Collections.singletonList(left.getValue()),
                Collections.singletonList(right.getValue()), entryPath));
        return true;
    }

    /**
     * Compares two attribute values, or two lists of attribute values.
     *
     * @param left  the value on the saved side
     * @param right the value on the loaded side
     * @param path  the path of the value, for the report
     * @return true when the values are equal
     */
    private boolean compareValues(Object left, Object right, String path) {
        if (Objects.equals(left, right)) {
            return true;
        }
        return fail("value", path + ": '" + left + "', loaded '" + right + "'");
    }

    /**
     * Compares the targets of one cross-reference after every contained object has
     * been matched.
     *
     * @param pending the targets on both sides
     * @return true when every target corresponds
     */
    private boolean comparePendingReference(PendingReference pending) {
        if (pending.leftTargets.size() != pending.rightTargets.size()) {
            String category = allDangling(pending.leftTargets) ? "dangling-reference" : "reference";
            return fail(category, pending.path + ": " + pending.leftTargets.size() + " targets, loaded "
                    + pending.rightTargets.size());
        }
        for (int index = 0; index < pending.leftTargets.size(); index++) {
            EObject left = (EObject) pending.leftTargets.get(index);
            EObject right = (EObject) pending.rightTargets.get(index);
            if (!targetsCorrespond(left, right)) {
                return fail(referenceCategory(left, right), pending.path + "[" + index + "]: refers to "
                        + describeTarget(left, leftBase) + ", loaded " + describeTarget(right, rightBase));
            }
        }
        return true;
    }

    /**
     * Tells whether two targets of a cross-reference correspond: the loaded target
     * is the match of the saved one, or both name the same object in another file.
     *
     * @param left  the target on the saved side
     * @param right the target on the loaded side
     * @return true when the targets correspond
     */
    private boolean targetsCorrespond(EObject left, EObject right) {
        if (left == null || right == null) {
            return left == right;
        }
        EObject match = matches.get(left);
        if (match != null) {
            return match == right;
        }
        return describeTarget(left, leftBase).equals(describeTarget(right, rightBase));
    }

    /**
     * Chooses the category of two targets that do not correspond. When the saved
     * target and the loaded target are different objects with the same identifier,
     * the file declares the identifier twice, and the load resolved the reference
     * to the other declaration.
     *
     * @param left  the target on the saved side
     * @param right the target on the loaded side
     * @return {@code dangling-reference}, {@code duplicate-id} or {@code reference}
     */
    private String referenceCategory(EObject left, EObject right) {
        if (isDangling(left)) {
            return "dangling-reference";
        }
        boolean bothObjects = left != null && right != null && !left.eIsProxy() && !right.eIsProxy();
        if (bothObjects && Objects.equals(EcoreUtil.getID(left), EcoreUtil.getID(right))) {
            return "duplicate-id";
        }
        return "reference";
    }

    /**
     * Chooses the category of a difference in the set state of a feature.
     *
     * @param left    the object on the saved side
     * @param feature the feature
     * @return the category
     */
    private String categoryOf(EObject left, EStructuralFeature feature) {
        if (FeatureMapUtil.isFeatureMap(feature)) {
            return featureMapCategory(feature);
        }
        if (feature instanceof EReference && !((EReference) feature).isContainment()) {
            List<?> targets = storedValues(left.eGet(feature, false));
            return !targets.isEmpty() && allDangling(targets) ? "dangling-reference" : "reference";
        }
        return feature instanceof EAttribute ? "value" : "content";
    }

    /**
     * Chooses the category of a difference inside a feature map.
     *
     * @param feature the feature map
     * @return {@code extension-attribute} for the attributes of vendor namespaces,
     *         otherwise {@code feature-map}
     */
    private static String featureMapCategory(EStructuralFeature feature) {
        return EXTENSION_ATTRIBUTES.equals(feature.getName()) ? "extension-attribute" : "feature-map";
    }

    /**
     * Tells whether every target in a list is a dangling proxy.
     *
     * @param targets the targets on the saved side
     * @return true when the list holds dangling proxies only
     */
    private boolean allDangling(List<?> targets) {
        for (Object target : targets) {
            if (!isDangling((EObject) target)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a target is a proxy into the model's own file that the local
     * resolution could not resolve, so the identifier it names exists nowhere in
     * the file.
     *
     * @param target the target on the saved side
     * @return true for a dangling proxy
     */
    private boolean isDangling(EObject target) {
        if (target == null || !target.eIsProxy()) {
            return false;
        }
        URI file = ((InternalEObject) target).eProxyURI().trimFragment();
        return file.equals(sourceUri) || file.equals(leftBase);
    }

    /**
     * Describes the target of a reference by file name and fragment. A reference
     * into the own resource is described by its fragment alone, because the
     * resource URI differs between the saved and the loaded side.
     *
     * @param target the target object or proxy
     * @param base   the URI of the resource that holds the reference
     * @return the description
     */
    private static String describeTarget(EObject target, URI base) {
        if (target == null) {
            return "nothing";
        }
        URI uri = target.eIsProxy() ? ((InternalEObject) target).eProxyURI() : EcoreUtil.getURI(target);
        if (uri == null) {
            return "an object outside any resource";
        }
        URI file = uri.trimFragment();
        if (file.equals(base) || file.isEmpty()) {
            return "#" + uri.fragment();
        }
        String lastSegment = file.lastSegment();
        String fileName = lastSegment == null || lastSegment.isEmpty() ? file.toString() : lastSegment;
        return fileName + "#" + uri.fragment();
    }

    /**
     * Finds the feature of the loaded class that matches a feature of the saved
     * class. Classes created on demand for unknown XML elements are different
     * objects after a load, so their features are matched by name.
     *
     * @param rightClass the class of the loaded object
     * @param feature    the feature of the saved object
     * @return the matching feature, or null
     */
    private static EStructuralFeature matchingFeature(EClass rightClass, EStructuralFeature feature) {
        if (rightClass == feature.getEContainingClass() || rightClass.getEAllStructuralFeatures().contains(feature)) {
            return feature;
        }
        return rightClass.getEStructuralFeature(feature.getName());
    }

    /**
     * Tells whether a feature is stored by a serialization.
     *
     * @param feature the feature
     * @return false for derived and transient features
     */
    private static boolean isStored(EStructuralFeature feature) {
        return !feature.isDerived() && !feature.isTransient();
    }

    /**
     * Returns the values of a cross-reference without resolving proxies.
     *
     * @param value the value read with {@code eGet(feature, false)}
     * @return the targets as a list
     */
    private static List<?> storedValues(Object value) {
        if (value instanceof InternalEList) {
            return ((InternalEList<?>) value).basicList();
        }
        return asList(value);
    }

    /**
     * Wraps a single value into a list, or returns a list value as it is.
     *
     * @param value a single value or a list
     * @return the values as a list
     */
    private static List<?> asList(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        }
        return Collections.singletonList(value);
    }

    /**
     * Identifies a class across loads by namespace and name.
     *
     * @param eClass the class
     * @return the key of the class
     */
    private static String classKey(EClass eClass) {
        String namespace = eClass.getEPackage() == null ? "" : eClass.getEPackage().getNsURI();
        return namespace + "#" + eClass.getName();
    }

    /**
     * Identifies a feature of a feature map across loads by its XML namespace and
     * XML name.
     *
     * @param feature the feature of an entry
     * @return the key of the feature
     */
    private static String featureKey(EStructuralFeature feature) {
        String namespace = ExtendedMetaData.INSTANCE.getNamespace(feature);
        return (namespace == null ? "" : namespace + "#") + ExtendedMetaData.INSTANCE.getName(feature);
    }

    /**
     * Returns the identifier of an object for the report.
     *
     * @param object the object
     * @return the identifier in braces, or an empty string
     */
    private static String idSuffix(EObject object) {
        String id = EcoreUtil.getID(object);
        return id == null ? "" : "{" + id + "}";
    }

    /**
     * Records the first difference with its category. A long path keeps its end,
     * which names the object and the feature that differ.
     *
     * @param category the category of the difference
     * @param text     the description of the difference
     * @return always false, so callers can return the result
     */
    private boolean fail(String category, String text) {
        String singleLine = Failures.clean(text, Integer.MAX_VALUE);
        int start = Math.max(0, singleLine.length() - MAXIMUM_DIFFERENCE_LENGTH);
        difference = category + " | " + (start > 0 ? "..." : "") + singleLine.substring(start);
        return false;
    }

    /**
     * The targets of one cross-reference on both sides, kept until every contained
     * object has been matched.
     */
    private static final class PendingReference {

        private final List<?> leftTargets;
        private final List<?> rightTargets;
        private final String path;

        /**
         * Keeps the targets of one reference.
         *
         * @param leftTargets  the targets on the saved side
         * @param rightTargets the targets on the loaded side
         * @param path         the path of the reference, for the report
         */
        private PendingReference(List<?> leftTargets, List<?> rightTargets, String path) {
            this.leftTargets = new ArrayList<>(leftTargets);
            this.rightTargets = new ArrayList<>(rightTargets);
            this.path = path;
        }
    }
}
