package org.ecorefs.eval.addprobe;

import java.io.IOException;
import java.nio.file.Path;

/**
 * One way of adding a file to a Kubo node through the HTTP API. The probe times
 * several implementations on the same file to find where the fixed cost of a
 * save comes from.
 */
public interface AddRequest {

    /**
     * Adds the file to the node and waits for the answer of the node.
     *
     * @param file the file to add
     * @return the CID that the node reports
     * @throws IOException when the request fails
     */
    String add(Path file) throws IOException;

    /**
     * Names the implementation in the output of the probe.
     *
     * @return a short name, for example {@code library}
     */
    String name();
}
