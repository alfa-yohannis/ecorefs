package org.ecorefs.eval.baselines;

import java.nio.file.Path;

/**
 * Creates the stores of a run by name.
 */
public final class StoreCatalog {

    /** No instances; the class only offers a static factory. */
    private StoreCatalog() {
    }

    /**
     * Creates one store.
     *
     * @param name     the store name: {@code xmi}, {@code ipfs}, {@code git},
     *                 {@code share} or {@code neoemf}
     * @param settings the settings of the run
     * @param work     the work folder of the run
     * @return the store
     * @throws IllegalArgumentException for an unknown name
     */
    public static ModelStore create(String name, BenchmarkSettings settings, Path work) {
        switch (name.trim()) {
            case "xmi":
                return new XmiFileStore(work);
            case "ipfs":
                return new IpfsStore(settings.ipfsAddress());
            case "git":
                return new GitStore(work, settings.gitPort());
            case "share":
                return new NetworkShareStore(work, settings.sshPort());
            case "neoemf":
                return new NeoEmfStore(work, settings.neoEmfMapping());
            default:
                throw new IllegalArgumentException("Unknown store " + name);
        }
    }
}
