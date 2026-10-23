package org.ecorefs.ipfs;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves folders of the ecorefs repository for the tests, so the tests run
 * wherever the repository is checked out. Surefire passes the folder of
 * core-plugin as the system property {@code basedir}, and an IDE starts the
 * tests in that folder, so the repository root is the parent of either one.
 */
final class RepositoryPaths {

    /** Root folder of the ecorefs repository. */
    private static final Path ROOT = Paths.get(System.getProperty("basedir", "")).toAbsolutePath().getParent();

    /** Prevents instances, because the class only resolves paths. */
    private RepositoryPaths() {
    }

    /**
     * Resolves a path inside the repository.
     *
     * @param relativePath path from the repository root, for example {@code eval/results}
     * @return the absolute path
     */
    static Path resolve(String relativePath) {
        return ROOT.resolve(relativePath);
    }
}
