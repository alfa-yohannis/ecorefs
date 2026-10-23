package org.ecorefs.eval.rq1;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ecorefs.eval.bpmn.LocalProxyResolver;
import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Runs one persistence path on one model file: a save of a fresh load, a load of
 * the stored content in a fresh resource set, a comparison of the two models, and
 * a second save of another fresh load to check that the same model receives the
 * same identifier.
 */
final class PathTrial {

    /** Column suffixes of one path in the result table. */
    static final List<String> COLUMNS = Arrays.asList("save", "load", "equal", "difference", "repeat", "save_ms",
            "load_ms", "id");

    private static final String NOT_RUN = "-";

    private final PersistencePath path;
    private final SourceModelLoader loader;
    private final Path modelFile;

    private String save = NOT_RUN;
    private String load = NOT_RUN;
    private String equal = NOT_RUN;
    private String difference = "";
    private String repeat = NOT_RUN;
    private String saveMillis = "";
    private String loadMillis = "";
    private String identifier = "";

    /**
     * Prepares the trial.
     *
     * @param path      the persistence path
     * @param loader    the loader of the source file
     * @param modelFile the BPMN file
     */
    PathTrial(PersistencePath path, SourceModelLoader loader, Path modelFile) {
        this.path = path;
        this.loader = loader;
        this.modelFile = modelFile;
    }

    /**
     * Runs the trial.
     *
     * @return the columns of the path in the order of {@link #COLUMNS}
     */
    List<String> run() {
        SavedModel saved = saveFreshLoad();
        if (saved != null) {
            loadAndCompare(saved);
            saveAgain(saved);
            path.discard(saved);
        }
        return Arrays.asList(save, load, equal, difference, repeat, saveMillis, loadMillis, identifier);
    }

    /**
     * Saves a fresh load of the file.
     *
     * @return the outcome, or null when the save failed
     */
    private SavedModel saveFreshLoad() {
        try {
            SavedModel saved = path.save(loader.load(modelFile));
            save = "OK";
            saveMillis = millis(saved.saveNanos);
            identifier = saved.identifier;
            return saved;
        } catch (Exception | StackOverflowError e) {
            save = "FAIL " + Failures.describe(e);
            return null;
        }
    }

    /**
     * Loads the stored content in a fresh resource set and compares it with the
     * saved model. A load that reports errors but yields content is compared too.
     *
     * @param saved the outcome of the save
     */
    private void loadAndCompare(SavedModel saved) {
        Resource loaded = path.createLoadTarget(saved);
        Throwable failure = null;
        long start = System.nanoTime();
        try {
            loaded.load(path.loadOptions());
        } catch (Exception | StackOverflowError e) {
            failure = e;
        }
        loadMillis = millis(System.nanoTime() - start);
        if (loaded.getContents().isEmpty()) {
            load = "FAIL " + (failure == null ? "no content" : Failures.describe(failure));
            return;
        }
        load = loadStatus(loaded, failure);
        LocalProxyResolver.resolveLocalProxies(loaded);
        String firstDifference = ModelComparer.findDifference(saved.resource, loaded,
                URI.createFileURI(modelFile.toAbsolutePath().toString()));
        equal = firstDifference == null ? "yes" : "no";
        difference = firstDifference == null ? "" : firstDifference;
        loaded.unload();
    }

    /**
     * Describes a load that yielded content.
     *
     * @param loaded  the loaded resource
     * @param failure the exception of the load, or null
     * @return OK, or the number of errors with the first one
     */
    private static String loadStatus(Resource loaded, Throwable failure) {
        if (loaded.getErrors().isEmpty() && failure == null) {
            return "OK";
        }
        String firstError = loaded.getErrors().isEmpty() ? Failures.describe(failure)
                : Failures.clean(loaded.getErrors().get(0).getMessage());
        return "ERRORS " + loaded.getErrors().size() + " " + firstError;
    }

    /**
     * Saves another fresh load of the file and compares the identifiers.
     *
     * @param first the outcome of the first save
     */
    private void saveAgain(SavedModel first) {
        try {
            SavedModel second = path.save(loader.load(modelFile));
            repeat = first.identifier.equals(second.identifier) ? "same" : "different";
            path.discard(second);
        } catch (Exception | StackOverflowError e) {
            repeat = "FAIL " + Failures.describe(e);
        }
    }

    /**
     * Formats a duration in milliseconds with three decimals.
     *
     * @param nanos the duration in nanoseconds
     * @return the formatted milliseconds
     */
    static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
    }
}
