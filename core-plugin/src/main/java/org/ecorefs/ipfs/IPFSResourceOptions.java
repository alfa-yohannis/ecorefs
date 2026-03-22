package org.ecorefs.ipfs;

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
}
