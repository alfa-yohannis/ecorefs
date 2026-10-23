package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * One way to store a model and to read it back: plain XMI or XMI with extended
 * metadata, BPMN 2.0 XML, each as a local file or on IPFS. A path receives a
 * freshly loaded BPMN resource for every save, so two saves of the same file never
 * share state.
 */
abstract class PersistencePath {

    private final String name;
    final ExperimentResourceSets resourceSets;

    /**
     * Creates a path.
     *
     * @param name         the column prefix of the path in the result table
     * @param resourceSets the builder of the resource sets
     */
    PersistencePath(String name, ExperimentResourceSets resourceSets) {
        this.name = name;
        this.resourceSets = resourceSets;
    }

    /**
     * Returns the column prefix of the path.
     *
     * @return the name, such as {@code xmi_ipfs}
     */
    final String name() {
        return name;
    }

    /**
     * Stores a model.
     *
     * @param source a freshly loaded BPMN resource whose local proxies are resolved
     * @return the outcome of the save
     * @throws IOException when the content cannot be written or added
     */
    abstract SavedModel save(Resource source) throws IOException;

    /**
     * Creates the resource that reads the stored content back, in a fresh resource set.
     *
     * @param saved the outcome of the save
     * @return the resource, not yet loaded
     */
    abstract Resource createLoadTarget(SavedModel saved);

    /**
     * Returns the load options of the path.
     *
     * @return the options
     */
    abstract Map<Object, Object> loadOptions();

    /**
     * Removes what the save left behind, such as a temporary file. Content on the
     * node stays, because IPFS has no delete.
     *
     * @param saved the outcome of the save
     */
    void discard(SavedModel saved) {
        saved.resource.unload();
    }
}
