package org.ecorefs.eval.rq1;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.ipfs.api.NamedStreamable;

/**
 * Builds the directory tree of a collection for a single add on the node. The tree
 * holds only the model files and the folders above them, so the clone metadata of
 * the collection, such as the {@code .git} folder, stays out of the directory.
 */
final class DirectoryTree {

    private final String name;
    private final Map<String, DirectoryTree> folders = new TreeMap<>();
    private final Map<String, Path> files = new TreeMap<>();

    /**
     * Creates an empty folder.
     *
     * @param name the name of the folder
     */
    private DirectoryTree(String name) {
        this.name = name;
    }

    /**
     * Builds the tree of a collection.
     *
     * @param models the files of the collection
     * @return the directory for the add
     */
    static NamedStreamable of(ModelList models) {
        DirectoryTree root = new DirectoryTree(models.collection);
        for (Path file : models.files) {
            root.add(models.collectionFolder.relativize(file), file);
        }
        return root.toStreamable();
    }

    /**
     * Adds a file below this folder.
     *
     * @param relativePath the path of the file relative to this folder
     * @param file         the file on disk
     */
    private void add(Path relativePath, Path file) {
        if (relativePath.getNameCount() == 1) {
            files.put(relativePath.toString(), file);
            return;
        }
        String folderName = relativePath.getName(0).toString();
        DirectoryTree folder = folders.get(folderName);
        if (folder == null) {
            folder = new DirectoryTree(folderName);
            folders.put(folderName, folder);
        }
        folder.add(relativePath.subpath(1, relativePath.getNameCount()), file);
    }

    /**
     * Converts the folder into the directory type of the IPFS client.
     *
     * @return the directory with its folders and files
     */
    private NamedStreamable toStreamable() {
        List<NamedStreamable> children = new ArrayList<>();
        for (DirectoryTree folder : folders.values()) {
            children.add(folder.toStreamable());
        }
        for (Path file : files.values()) {
            children.add(new NamedStreamable.FileWrapper(file.toFile()));
        }
        return new NamedStreamable.DirWrapper(name, children);
    }
}
