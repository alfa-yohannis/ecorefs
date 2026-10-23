package org.ecorefs.eval.baselines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Deletes folders with their content, outside the measured regions.
 */
public final class FileTrees {

    /** No instances; the class only offers a static helper. */
    private FileTrees() {
    }

    /**
     * Deletes a file or a folder with everything inside. A missing path is fine.
     *
     * @param root the file or folder to delete
     * @throws IOException when a file cannot be deleted
     */
    public static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(root)) {
            paths = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    /**
     * Returns the total size of the files below a folder.
     *
     * @param root the folder
     * @return the size in bytes
     * @throws IOException when the folder cannot be read
     */
    public static long sizeOf(Path root) throws IOException {
        long total = 0;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        for (Path file : files) {
            total += Files.size(file);
        }
        return total;
    }
}
