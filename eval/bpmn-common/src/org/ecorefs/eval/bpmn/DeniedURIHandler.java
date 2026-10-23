package org.ecorefs.eval.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;

/**
 * Refuses every URI that reaches it. The handler sits last in the handler list of
 * each resource set of the experiment, so a model from the dataset can never make
 * the experiment read a web address or a file outside the folders it owns, even
 * when a reference or an import of the model names one.
 */
public final class DeniedURIHandler extends URIHandlerImpl {

    /**
     * Accepts every URI, because the handler is the last one asked.
     *
     * @param uri the URI to read or write
     * @return always true
     */
    @Override
    public boolean canHandle(URI uri) {
        return true;
    }

    /**
     * Refuses to read.
     *
     * @param uri     the URI to read
     * @param options the load options
     * @return never
     * @throws IOException always, naming the refused URI
     */
    @Override
    public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
        throw new IOException("The experiment does not read " + uri);
    }

    /**
     * Refuses to write.
     *
     * @param uri     the URI to write
     * @param options the save options
     * @return never
     * @throws IOException always, naming the refused URI
     */
    @Override
    public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
        throw new IOException("The experiment does not write " + uri);
    }

    /**
     * Reports every refused URI as missing, without trying to open it.
     *
     * @param uri     the URI to check
     * @param options the options of the check
     * @return always false
     */
    @Override
    public boolean exists(URI uri, Map<?, ?> options) {
        return false;
    }

    /**
     * Reports no attributes, without trying to open the URI.
     *
     * @param uri     the URI to inspect
     * @param options the options of the request
     * @return an empty map
     */
    @Override
    public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
        return Collections.emptyMap();
    }
}
