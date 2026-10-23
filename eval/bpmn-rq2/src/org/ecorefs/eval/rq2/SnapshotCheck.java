package org.ecorefs.eval.rq2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ecorefs.eval.bpmn.KuboNode;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Checks whether the stored resources form a consistent snapshot, where every
 * reference names the current CID of its target. The check reads the bytes of
 * every resource back from the node by its current CID and classifies every
 * {@code ipfs} URI in them: the current CID of a resource, an older CID of a
 * resource, or a URI that names no saved version, such as a placeholder.
 */
final class SnapshotCheck {

    private static final Pattern IPFS_URI = Pattern.compile("ipfs://([^#\"\\s/]+)");

    int currentReferences;
    int staleReferences;
    int unknownReferences;
    int pairsWithStaleReferences;

    /**
     * Runs the check.
     *
     * @param node      the node that stores the resources
     * @param resources the resources of the snapshot
     * @param history   the CIDs that the resources have had
     * @return the counts
     * @throws IOException when the node does not return a resource
     */
    static SnapshotCheck run(KuboNode node, List<Resource> resources, CidHistory history) throws IOException {
        SnapshotCheck check = new SnapshotCheck();
        for (Resource holder : resources) {
            String stored = new String(node.cat(CidHistory.currentCid(holder)), StandardCharsets.UTF_8);
            check.classifyReferencesOf(holder, stored, history);
        }
        return check;
    }

    /**
     * Classifies the {@code ipfs} URIs in the stored bytes of one resource.
     *
     * @param holder  the resource whose bytes are read
     * @param stored  the stored XMI text
     * @param history the CIDs that the resources have had
     */
    private void classifyReferencesOf(Resource holder, String stored, CidHistory history) {
        Set<Resource> staleTargets = new HashSet<>();
        Matcher matcher = IPFS_URI.matcher(stored);
        while (matcher.find()) {
            String cid = matcher.group(1);
            Resource target = history.resourceOf(cid);
            if (target == null) {
                unknownReferences++;
            } else if (cid.equals(CidHistory.currentCid(target))) {
                currentReferences++;
            } else {
                staleReferences++;
                staleTargets.add(target);
            }
        }
        pairsWithStaleReferences += staleTargets.size();
    }

    /**
     * Tells whether every stored reference names a current CID.
     *
     * @return true for a consistent snapshot
     */
    boolean consistent() {
        return staleReferences == 0 && unknownReferences == 0;
    }
}
