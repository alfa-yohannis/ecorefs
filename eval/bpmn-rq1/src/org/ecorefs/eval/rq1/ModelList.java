package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The model files of one collection, read from a list with one absolute path per
 * line, as {@code run.sh} writes it with {@code find}.
 */
final class ModelList {

    final String collection;
    final Path collectionFolder;
    final List<Path> files;

    /**
     * Keeps the files of a collection.
     *
     * @param collection       the name of the collection
     * @param collectionFolder the folder of the collection
     * @param files            the model files
     */
    private ModelList(String collection, Path collectionFolder, List<Path> files) {
        this.collection = collection;
        this.collectionFolder = collectionFolder;
        this.files = files;
    }

    /**
     * Reads the list of a collection.
     *
     * @param datasetRoot the folder that holds the collections
     * @param collection  the name of the collection
     * @param listFile    the list with one path per line
     * @param limit       the largest number of files to keep, or 0 for all
     * @return the model list
     * @throws IOException when the list cannot be read
     */
    static ModelList read(Path datasetRoot, String collection, Path listFile, int limit) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String line : Files.readAllLines(listFile, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && (limit == 0 || files.size() < limit)) {
                files.add(Path.of(line));
            }
        }
        return new ModelList(collection, datasetRoot.resolve(collection), files);
    }

    /**
     * Returns the path of a file inside its collection.
     *
     * @param file a file of the list
     * @return the relative path
     */
    String relativePath(Path file) {
        return collectionFolder.relativize(file).toString();
    }
}
