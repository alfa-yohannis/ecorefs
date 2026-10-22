package org.ecorefs.ipfs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Option keys for IPFS-backed EMF resource save/load operations.
 * These keys are passed through the standard EMF {@code Map<?, ?>} options parameter.
 *
 * <p>Example usage:
 * <pre>{@code
 * Map<String, Object> options = new HashMap<>();
 * options.put(IPFSResourceOptions.REFERENCE_MODE, ReferenceMode.IPNS);
 * resource.save(options);
 * }</pre>
 */
public final class IPFSResourceOptions {

    private IPFSResourceOptions() {
        // Non-instantiable constants class
    }

    /**
     * Save option key to select the reference mode.
     * Value must be a {@link ReferenceMode} enum constant.
     * Defaults to {@link ReferenceMode#CID} if not specified.
     */
    public static final String REFERENCE_MODE = "org.ecorefs.ipfs.referenceMode";

    // EcoreFS begin: location attributes such as BPMN import locations
    /**
     * Save option key for the updaters of location attributes. The value is a
     * collection of {@link LocationAttributeUpdater} objects. Before a resource is
     * serialized, every updater writes the current URIs of the named resources into
     * the location attributes of the resource, and a cascade save follows those
     * attributes like references. Without the option, location attributes keep
     * their values.
     */
    public static final String LOCATION_UPDATERS = "org.ecorefs.ipfs.locationUpdaters";

    /**
     * Reads the updaters of location attributes from save options.
     *
     * @param options the save options, or null
     * @return the updaters, or an empty list when the options name none
     * @throws IllegalArgumentException when the option holds something other than a
     *                                  collection of updaters
     */
    public static List<LocationAttributeUpdater> locationUpdaters(Map<?, ?> options) {
        List<LocationAttributeUpdater> updaters = new ArrayList<>();
        Object value = options == null ? null : options.get(LOCATION_UPDATERS);
        if (value == null) {
            return updaters;
        }
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException(LOCATION_UPDATERS
                    + " needs a collection of LocationAttributeUpdater, not " + value.getClass().getName());
        }
        for (Object element : (Collection<?>) value) {
            updaters.add(asUpdater(element));
        }
        return updaters;
    }

    /**
     * Checks that one element of the option is an updater.
     *
     * @param element an element of the option value
     * @return the element as an updater
     * @throws IllegalArgumentException when the element is no updater
     */
    private static LocationAttributeUpdater asUpdater(Object element) {
        if (element instanceof LocationAttributeUpdater) {
            return (LocationAttributeUpdater) element;
        }
        throw new IllegalArgumentException(LOCATION_UPDATERS + " holds " + element
                + ", which is no LocationAttributeUpdater");
    }
    // EcoreFS end
}
