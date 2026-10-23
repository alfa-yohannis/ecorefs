package org.ecorefs.eval.bpmn;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.FileURIHandlerImpl;

/**
 * Reads and writes files only inside the folders that the experiment owns: the
 * dataset and the temporary folder of the run. A file URI outside these folders
 * falls through to {@link DeniedURIHandler}.
 */
public final class ExperimentFileURIHandler extends FileURIHandlerImpl {

    private final List<Path> allowedFolders = new ArrayList<>();

    /**
     * Creates the handler for the given folders.
     *
     * @param allowedFolders the folders whose files the experiment may read and write
     */
    public ExperimentFileURIHandler(List<Path> allowedFolders) {
        for (Path folder : allowedFolders) {
            this.allowedFolders.add(folder.toAbsolutePath().normalize());
        }
    }

    /**
     * Accepts absolute file URIs inside one of the allowed folders.
     *
     * @param uri the URI to read or write
     * @return true when the URI names a file inside an allowed folder
     */
    @Override
    public boolean canHandle(URI uri) {
        if (!uri.isFile() || uri.isRelative()) {
            return false;
        }
        Path file = Paths.get(uri.toFileString()).toAbsolutePath().normalize();
        return isInsideAllowedFolder(file);
    }

    /**
     * Checks whether a normalized path lies below one of the allowed folders.
     *
     * @param file the normalized absolute path
     * @return true when an allowed folder contains the path
     */
    private boolean isInsideAllowedFolder(Path file) {
        for (Path folder : allowedFolders) {
            if (file.startsWith(folder)) {
                return true;
            }
        }
        return false;
    }
}
