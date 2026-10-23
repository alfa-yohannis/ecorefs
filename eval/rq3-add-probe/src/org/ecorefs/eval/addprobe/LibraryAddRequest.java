package org.ecorefs.eval.addprobe;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Adds a file through the Java IPFS client, in the same call that
 * IPFSResourceImpl.saveIPFS makes. The client sends the request with
 * "Expect: 100-continue" and writes the multipart body in chunks.
 */
public final class LibraryAddRequest implements AddRequest {

    /** Client of the HTTP API of the node. */
    private final IPFS ipfs;

    /**
     * Connects the client to the node.
     *
     * @param apiAddress multiaddress of the API, for example {@code /ip4/127.0.0.1/tcp/5001}
     */
    public LibraryAddRequest(String apiAddress) {
        this.ipfs = new IPFS(apiAddress);
    }

    /**
     * Adds the file with {@code IPFS.add}, as the save of the resource does.
     *
     * @param file the file to add
     * @return the CID that the node reports
     * @throws IOException when the request fails
     */
    @Override
    public String add(Path file) throws IOException {
        MerkleNode result = ipfs.add(new NamedStreamable.FileWrapper(file.toFile())).get(0);
        return result.hash.toBase58();
    }

    /**
     * Names the implementation in the output of the probe.
     *
     * @return {@code library}
     */
    @Override
    public String name() {
        return "library";
    }
}
