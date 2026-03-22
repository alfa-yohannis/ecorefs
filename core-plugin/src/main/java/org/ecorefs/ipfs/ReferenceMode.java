package org.ecorefs.ipfs;

/**
 * Defines the reference strategy for cross-resource links in IPFS-backed EMF models.
 *
 * <ul>
 *   <li>{@link #CID} — Immutable, content-addressed references. Changing a resource's content
 *       produces a new CID, requiring all referencing resources to be cascade-saved.</li>
 *   <li>{@link #IPNS} — Mutable IPNS name references. Each resource is assigned a stable
 *       IPNS key that is re-published to point to the latest CID. No cascade is needed.</li>
 * </ul>
 */
public enum ReferenceMode {

    /** Immutable content-addressed references. Triggers cascade save on update. */
    CID,

    /** Mutable IPNS name references. No cascade needed — pointers are updated in place. */
    IPNS
}
