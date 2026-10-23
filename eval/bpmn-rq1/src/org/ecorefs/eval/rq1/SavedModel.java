package org.ecorefs.eval.rq1;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * The outcome of one save: the resource as it was in memory after the save, the
 * location the content went to, the identifier of the stored bytes, and the time
 * the save took.
 */
final class SavedModel {

    final Resource resource;
    final URI location;
    final String identifier;
    final long saveNanos;

    /**
     * Keeps the outcome of a save.
     *
     * @param resource   the saved resource, used as the expected side of the comparison
     * @param location   the file URI or the {@code ipfs} URI of the stored content
     * @param identifier the CID, or the SHA-256 digest of a local file
     * @param saveNanos  the duration of the save in nanoseconds
     */
    SavedModel(Resource resource, URI location, String identifier, long saveNanos) {
        this.resource = resource;
        this.location = location;
        this.identifier = identifier;
        this.saveNanos = saveNanos;
    }
}
