package org.ecorefs.eval.bpmn;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

/**
 * The Kubo node of the experiment, reached through the Java client that the core
 * plugin uses. The class adds bytes and directories, computes CIDs without
 * storing, and reads the statistics of the repository.
 */
public final class KuboNode {

    private final IPFS ipfs;

    /**
     * Connects to a node.
     *
     * @param multiaddress the API address, such as {@code /ip4/127.0.0.1/tcp/5101}
     */
    public KuboNode(String multiaddress) {
        this.ipfs = new IPFS(multiaddress);
    }

    /**
     * Returns the client, for the IPFS resources and URI handlers of the core plugin.
     *
     * @return the client
     */
    public IPFS client() {
        return ipfs;
    }

    /**
     * Stores bytes on the node.
     *
     * @param bytes the content
     * @return the CID of the content
     * @throws IOException when the node refuses the content
     */
    public String add(byte[] bytes) throws IOException {
        return ipfs.add(new NamedStreamable.ByteArrayWrapper(bytes), false, false).get(0).hash.toBase58();
    }

    /**
     * Reads the content stored under a CID.
     *
     * @param cid the CID
     * @return the content
     * @throws IOException when the node does not hold the content
     */
    public byte[] cat(String cid) throws IOException {
        return ipfs.cat(Multihash.fromBase58(cid));
    }

    /**
     * Stores a directory tree on the node.
     *
     * @param directory the directory with its files
     * @return the CID of the directory
     * @throws IOException when the node refuses the content
     */
    public String addDirectory(NamedStreamable directory) throws IOException {
        List<MerkleNode> nodes = ipfs.add(directory, false, false);
        String directoryName = directory.getName().orElse("");
        for (MerkleNode node : nodes) {
            if (directoryName.equals(node.name.orElse(null))) {
                return node.hash.toBase58();
            }
        }
        return nodes.get(nodes.size() - 1).hash.toBase58();
    }

    /**
     * Reads the size and the object count of the repository.
     *
     * @return the statistics
     * @throws IOException when the node does not answer
     */
    public RepositoryStatistics repositoryStatistics() throws IOException {
        Map<?, ?> statistics = ipfs.repo.stat(false, false);
        return new RepositoryStatistics(toLong(statistics.get("RepoSize")), toLong(statistics.get("NumObjects")));
    }

    /**
     * Converts a number from the JSON answer of the node.
     *
     * @param value the number, or null
     * @return the value, or -1 when the answer had none
     */
    private static long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : -1;
    }

    /**
     * The size of the repository in bytes and its number of objects.
     */
    public static final class RepositoryStatistics {

        public final long sizeBytes;
        public final long objects;

        /**
         * Keeps the two values.
         *
         * @param sizeBytes the size of the repository in bytes
         * @param objects   the number of blocks in the repository
         */
        public RepositoryStatistics(long sizeBytes, long objects) {
            this.sizeBytes = sizeBytes;
            this.objects = objects;
        }
    }
}
