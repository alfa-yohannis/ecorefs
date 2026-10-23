package org.ecorefs.eval.baselines;

import fr.inria.atlanmod.neoemf.core.PersistenceFactory;
import fr.inria.atlanmod.neoemf.data.mapdb.config.MapDbConfig;
import fr.inria.atlanmod.neoemf.data.mapdb.util.MapDbUriFactory;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.resource.PersistentResourceFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * NeoEMF with the embedded MapDB back end as the database-backed store. The
 * save starts from the same plain in-memory copy as the other stores: it
 * creates the database, copies every element into it and commits. NeoEMF loads
 * elements lazily, so the load only opens the database and the visit fetches
 * the elements; a full read of the model is the load plus the visit. The
 * benchmark metamodel is copied into a second package under another namespace,
 * whose factory creates NeoEMF objects, so the other stores keep the plain
 * dynamic objects.
 */
public final class NeoEmfStore implements ModelStore {

    /** Namespace of the NeoEMF copy of the benchmark metamodel. */
    private static final String NAMESPACE = BenchmarkModels.NAMESPACE + "/neoemf";

    /** Folder of the databases. */
    private final Path directory;

    /** How the MapDB back end stores multi-valued features. */
    private final String mapping;

    /** Copies the plain objects into a NeoEMF resource. */
    private NeoEmfModelCopier copier;

    /** URI scheme of MapDB databases, read from the URI factory. */
    private String scheme;

    /** Number of elements of the current model size. */
    private int elementCount;

    /**
     * Creates the store.
     *
     * @param runDirectory the work folder of the run
     * @param mapping      the MapDB mapping: {@code indices}, {@code arrays} or {@code lists}
     */
    public NeoEmfStore(Path runDirectory, String mapping) {
        this.directory = runDirectory.resolve("neoemf");
        this.mapping = mapping;
    }

    /**
     * Returns the name of the store.
     *
     * @return {@code neoemf}
     */
    @Override
    public String name() {
        return "neoemf";
    }

    /**
     * Registers the NeoEMF copy of the metamodel and creates the folder of the
     * databases.
     *
     * @throws Exception when the folder cannot be created
     */
    @Override
    public void start() throws Exception {
        Files.createDirectories(directory);
        EPackage persistentPackage = BenchmarkModels.createPackage(NAMESPACE, "benchmarkneoemf");
        EPackage.Registry.INSTANCE.put(NAMESPACE, persistentPackage);
        PersistenceFactory.updateIfDynamic(persistentPackage);
        copier = new NeoEmfModelCopier(persistentPackage);
        scheme = new MapDbUriFactory().createLocalUri(directory.resolve("probe").toFile()).scheme();
        // Builds the options once, so an unknown mapping stops the run before any measurement.
        options();
    }

    /**
     * Remembers the model size for the names of the databases.
     *
     * @param elementCount number of elements of the models that follow
     */
    @Override
    public void prepareSize(int elementCount) {
        this.elementCount = elementCount;
    }

    /**
     * Saves the copy into a new MapDB database, opens the database in a fresh
     * resource set and visits every element.
     *
     * @param content   the root objects of the plain copy
     * @param iteration number of the iteration
     * @param times     receives the phases save, load, visit, write and read
     * @throws Exception when the save, the load or the visit fails
     */
    @Override
    public void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception {
        Path database = directory.resolve("model-" + elementCount + "-" + iteration);
        URI uri = new MapDbUriFactory().createLocalUri(database.toFile());
        Resource target = createResourceSet().createResource(uri);

        long start = System.nanoTime();
        target.save(options());
        copier.copyInto(content, target);
        target.save(options());
        ((PersistentResource) target).close();
        times.recordSince("save", start);
        times.recordSince("write", start);

        Resource loaded = createResourceSet().createResource(uri);
        start = System.nanoTime();
        loaded.load(options());
        loaded.getContents();
        times.recordSince("load", start);
        times.recordSince("read", start);

        start = System.nanoTime();
        VisitSummary visit = ModelTraversal.visitAll(loaded);
        times.recordSince("visit", start);
        times.recordVisit(visit);

        ((PersistentResource) loaded).close();
        FileTrees.deleteRecursively(database);
    }

    /**
     * Returns the options of every save and load: the MapDB back end with the
     * chosen mapping of multi-valued features.
     *
     * @return the NeoEMF configuration as EMF options
     * @throws IllegalArgumentException for an unknown mapping
     */
    private Map<String, ?> options() {
        switch (mapping) {
            case "indices":
                return new MapDbConfig().withIndices().toMap();
            case "arrays":
                return new MapDbConfig().withArrays().toMap();
            case "lists":
                return new MapDbConfig().withLists().toMap();
            default:
                throw new IllegalArgumentException("Unknown MapDB mapping " + mapping);
        }
    }

    /**
     * Builds a resource set that opens MapDB databases through NeoEMF.
     *
     * @return the configured resource set
     */
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put(scheme, PersistentResourceFactory.getInstance());
        return resourceSet;
    }

    /**
     * Needs no cleanup per model size.
     */
    @Override
    public void finishSize() {
        // The databases are deleted after every iteration.
    }

    /**
     * Needs no services.
     */
    @Override
    public void stop() {
        // The databases are closed after every iteration.
    }
}
