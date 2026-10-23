package org.ecorefs.eval.baselines;

import org.eclipse.emf.ecore.EObject;

import java.util.Collection;

/**
 * One way to store a model and to read the model back, measured by the
 * benchmark. Every store reports the phases of one iteration separately, for
 * example the commit and the push of a Git repository, so the result tables
 * show where the time goes.
 */
public interface ModelStore {

    /**
     * Returns the short name of the store, used in the result files.
     *
     * @return the name, for example {@code git}
     */
    String name();

    /**
     * Starts the services that the store needs for the whole run, such as a
     * server process or a container. Called once before the first model size.
     *
     * @throws Exception when a service cannot start
     */
    void start() throws Exception;

    /**
     * Prepares the store for the models of one size, for example by creating a
     * fresh repository.
     *
     * @param elementCount number of elements of the models that follow
     * @throws Exception when the preparation fails
     */
    void prepareSize(int elementCount) throws Exception;

    /**
     * Saves one copy of the model, loads the copy back in a fresh resource set
     * and visits every element of the loaded model, recording the duration of
     * every phase.
     *
     * @param content   the root objects of the copy, not yet attached to a resource
     * @param iteration number of the iteration, used to name files and commits
     * @param times     receives the duration of every phase
     * @throws Exception when a save, a load or a visit fails
     */
    void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception;

    /**
     * Removes what the store created for the current model size.
     *
     * @throws Exception when the cleanup fails
     */
    void finishSize() throws Exception;

    /**
     * Stops the services of the store. Called once after the last model size.
     */
    void stop();
}
