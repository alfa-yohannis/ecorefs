package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Compares persistence through a local XMI file with persistence through a local
 * IPFS node, for generated models of growing size.
 *
 * The measurement follows four rules. Every load runs in a fresh resource set,
 * so the load reads the file or fetches the content instead of returning a
 * resource that already sits in memory. The copy of the source model happens
 * outside the measured region, because the copy belongs to the test and not to
 * persistence. Each iteration stamps the copy with the iteration number, so
 * every iteration stores new content and receives a new content identifier.
 * Durations are kept in nanoseconds and reported in milliseconds with three
 * decimals, so differences below one millisecond stay visible. A garbage
 * collection runs between the iterations, outside the measured region, because
 * every iteration allocates a full copy of the model.
 */
public class FastPerformanceBenchmarkTest {

    /** Directory with the generated models of the benchmark. */
    private static final String SOURCE_DIRECTORY = "/data2/projects/ecorefs/eval/custom_generator/output/";

    /** File that receives one row per measured iteration. */
    private static final String RESULT_FILE = "/data2/projects/ecorefs/eval/results/benchmark_metrics.csv";

    /** Multiaddress of the local Kubo node. */
    private static final String IPFS_ADDRESS = "/ip4/127.0.0.1/tcp/5001";

    /** Namespace of the generated benchmark metamodel. */
    private static final String BENCHMARK_NAMESPACE = "http://benchmark/1.0";

    /** Element counts of the generated models. */
    private static final int[] ELEMENT_COUNTS = { 100, 1000, 10000, 100000, 500000 };

    /** Iterations that warm up the virtual machine and stay unrecorded. */
    private static final int WARMUP_ITERATIONS = 3;

    /** Iterations that enter the statistics. */
    private static final int MEASURED_ITERATIONS = 30;

    /** Nanoseconds in one millisecond. */
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    /** Client for the HTTP application programming interface of the local node. */
    private static IPFS ipfs;

    /**
     * Connects to the local IPFS node and registers the metamodel of the generated
     * models, so the models load without generated Java classes.
     */
    @BeforeAll
    public static void setUp() {
        ipfs = new IPFS(IPFS_ADDRESS);
        registerBenchmarkMetamodel();
    }

    /**
     * Creates the benchmark metamodel at runtime and puts the metamodel into the
     * global package registry. The metamodel holds one class with an integer
     * identifier and a containment reference to child elements.
     */
    private static void registerBenchmarkMetamodel() {
        EcoreFactory factory = EcoreFactory.eINSTANCE;
        EPackage benchmarkPackage = factory.createEPackage();
        benchmarkPackage.setName("benchmark");
        benchmarkPackage.setNsPrefix("bm");
        benchmarkPackage.setNsURI(BENCHMARK_NAMESPACE);

        EClass componentClass = factory.createEClass();
        componentClass.setName("Component");

        EAttribute identifierAttribute = factory.createEAttribute();
        identifierAttribute.setName("id");
        identifierAttribute.setEType(EcorePackage.eINSTANCE.getEInt());
        componentClass.getEStructuralFeatures().add(identifierAttribute);

        EReference childrenReference = factory.createEReference();
        childrenReference.setName("children");
        childrenReference.setEType(componentClass);
        childrenReference.setUpperBound(-1);
        childrenReference.setContainment(true);
        componentClass.getEStructuralFeatures().add(childrenReference);

        benchmarkPackage.getEClassifiers().add(componentClass);
        EPackage.Registry.INSTANCE.put(benchmarkPackage.getNsURI(), benchmarkPackage);
    }

    /**
     * Builds a resource set that reads and writes XMI files.
     *
     * @return a resource set with the default XMI factory
     */
    private ResourceSet createFileResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        return resourceSet;
    }

    /**
     * Builds a resource set that reads and writes resources on IPFS.
     *
     * @return a resource set with the IPFS factory and the IPFS URI handler
     */
    private ResourceSet createIpfsResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipfs", new IPFSResourceFactoryImpl(ipfs));
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    /**
     * Loads a generated model once per element count, so every iteration copies
     * the model from memory instead of reading the file again.
     *
     * @param sourcePath path of the generated XMI file
     * @return the loaded resource
     */
    private Resource loadSourceModel(String sourcePath) {
        ResourceSet sourceResourceSet = createFileResourceSet();
        return sourceResourceSet.getResource(URI.createFileURI(sourcePath), true);
    }

    /**
     * Copies the source model and stamps the copy with the iteration number, so
     * the serialized bytes differ between iterations.
     *
     * @param source    the loaded source model
     * @param iteration number of the current iteration
     * @return the root objects of the copy
     */
    private Collection<EObject> createIterationContent(Resource source, int iteration) {
        Collection<EObject> copy = EcoreUtil.copyAll(source.getContents());
        for (EObject root : copy) {
            EStructuralFeature identifierFeature = root.eClass().getEStructuralFeature("id");
            if (identifierFeature != null) {
                root.eSet(identifierFeature, iteration);
            }
        }
        return copy;
    }

    /**
     * Measures one save to a local XMI file. The content is attached to the
     * resource before the measurement, so the measurement covers the save alone.
     *
     * @param fileResource the resource that already holds the content
     * @return the elapsed time in nanoseconds
     * @throws IOException when the save fails
     */
    private long measureFileSave(Resource fileResource) throws IOException {
        long start = System.nanoTime();
        fileResource.save(Collections.emptyMap());
        return System.nanoTime() - start;
    }

    /**
     * Measures one load of a local XMI file in a fresh resource set.
     *
     * @param targetFile the file written by the preceding save
     * @return the elapsed time in nanoseconds
     */
    private long measureFileLoad(Path targetFile) {
        ResourceSet freshResourceSet = createFileResourceSet();
        long start = System.nanoTime();
        Resource loaded = freshResourceSet.getResource(URI.createFileURI(targetFile.toString()), true);
        loaded.getContents();
        return System.nanoTime() - start;
    }

    /**
     * Measures one save to the local IPFS node.
     *
     * @param ipfsResource the resource that already holds the content
     * @return the elapsed time in nanoseconds
     * @throws IOException when the save fails
     */
    private long measureIpfsSave(Resource ipfsResource) throws IOException {
        long start = System.nanoTime();
        ipfsResource.save(Collections.emptyMap());
        return System.nanoTime() - start;
    }

    /**
     * Measures one load from the local IPFS node in a fresh resource set.
     *
     * @param contentUri the URI assigned by the preceding save
     * @return the elapsed time in nanoseconds
     * @throws IOException when the load fails
     */
    private long measureIpfsLoad(URI contentUri) throws IOException {
        ResourceSet freshResourceSet = createIpfsResourceSet();
        long start = System.nanoTime();
        Resource loaded = freshResourceSet.createResource(contentUri);
        loaded.load(Collections.emptyMap());
        loaded.getContents();
        return System.nanoTime() - start;
    }

    /**
     * Converts a duration to milliseconds.
     *
     * @param nanoseconds the measured duration
     * @return the duration in milliseconds
     */
    private double toMilliseconds(long nanoseconds) {
        return nanoseconds / NANOS_PER_MILLISECOND;
    }

    /**
     * Computes the arithmetic mean of a series.
     *
     * @param values the measured durations in milliseconds
     * @return the mean value
     */
    private double mean(List<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Computes the median of a series.
     *
     * @param values the measured durations in milliseconds
     * @return the median value
     */
    private double median(List<Double> values) {
        double[] sorted = new double[values.size()];
        for (int index = 0; index < values.size(); index++) {
            sorted[index] = values.get(index);
        }
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[middle - 1] + sorted[middle]) / 2.0;
        }
        return sorted[middle];
    }

    /**
     * Computes the sample standard deviation of a series.
     *
     * @param values the measured durations in milliseconds
     * @return the standard deviation
     */
    private double standardDeviation(List<Double> values) {
        double average = mean(values);
        double sumOfSquares = 0.0;
        for (double value : values) {
            sumOfSquares += Math.pow(value - average, 2);
        }
        return Math.sqrt(sumOfSquares / (values.size() - 1));
    }

    /**
     * Runs the benchmark for every element count and writes one row per measured
     * iteration to the result file.
     *
     * @throws Exception when a save, a load or the result file fails
     */
    @Test
    public void compareFileAndIpfsPersistence() throws Exception {
        Files.createDirectories(Paths.get(RESULT_FILE).getParent());
        try (PrintWriter writer = new PrintWriter(RESULT_FILE)) {
            writer.println("Elements,Iteration,XmiSaveMs,IpfsSaveMs,XmiLoadMs,IpfsLoadMs");
            System.out.println("Elements\tXMI save\tIPFS save\tXMI load\tIPFS load (mean / median / SD in ms)");

            for (int elementCount : ELEMENT_COUNTS) {
                String sourcePath = SOURCE_DIRECTORY + "model_" + elementCount + ".xmi";
                if (!new File(sourcePath).exists()) {
                    System.out.println("Source missing: " + sourcePath);
                    continue;
                }
                measureOneModelSize(elementCount, sourcePath, writer);
            }
        }
    }

    /**
     * Runs the warm-up iterations and the measured iterations for one model size.
     *
     * @param elementCount number of elements in the generated model
     * @param sourcePath   path of the generated model
     * @param writer       writer of the result file
     * @throws IOException when a save, a load or the temporary file fails
     */
    private void measureOneModelSize(int elementCount, String sourcePath, PrintWriter writer) throws IOException {
        Resource source = loadSourceModel(sourcePath);
        List<Double> fileSaves = new ArrayList<>();
        List<Double> ipfsSaves = new ArrayList<>();
        List<Double> fileLoads = new ArrayList<>();
        List<Double> ipfsLoads = new ArrayList<>();

        for (int iteration = 0; iteration < WARMUP_ITERATIONS + MEASURED_ITERATIONS; iteration++) {
            Path temporaryFile = Files.createTempFile("ecorefs_benchmark_", ".xmi");
            ResourceSet fileResourceSet = createFileResourceSet();
            Resource fileResource = fileResourceSet.createResource(URI.createFileURI(temporaryFile.toString()));
            fileResource.getContents().addAll(createIterationContent(source, iteration));

            long fileSaveTime = measureFileSave(fileResource);
            long fileLoadTime = measureFileLoad(temporaryFile);

            ResourceSet ipfsResourceSet = createIpfsResourceSet();
            Resource ipfsResource = ipfsResourceSet.createResource(URI.createURI("ipfs://pending"));
            ipfsResource.getContents().addAll(fileResource.getContents());

            long ipfsSaveTime = measureIpfsSave(ipfsResource);
            long ipfsLoadTime = measureIpfsLoad(ipfsResource.getURI());

            Files.deleteIfExists(temporaryFile);
            // A collection between the iterations keeps the copies of the previous
            // iteration out of the next measurement.
            System.gc();

            boolean isWarmup = iteration < WARMUP_ITERATIONS;
            if (isWarmup) {
                continue;
            }
            int measuredIteration = iteration - WARMUP_ITERATIONS + 1;
            fileSaves.add(toMilliseconds(fileSaveTime));
            ipfsSaves.add(toMilliseconds(ipfsSaveTime));
            fileLoads.add(toMilliseconds(fileLoadTime));
            ipfsLoads.add(toMilliseconds(ipfsLoadTime));
            writer.printf("%d,%d,%.3f,%.3f,%.3f,%.3f%n", elementCount, measuredIteration,
                    toMilliseconds(fileSaveTime), toMilliseconds(ipfsSaveTime),
                    toMilliseconds(fileLoadTime), toMilliseconds(ipfsLoadTime));
            writer.flush();
        }

        System.out.printf("%d\t%s\t%s\t%s\t%s%n", elementCount, describe(fileSaves), describe(ipfsSaves),
                describe(fileLoads), describe(ipfsLoads));
    }

    /**
     * Formats the statistics of one series for the console output.
     *
     * @param values the measured durations in milliseconds
     * @return mean, median and standard deviation as text
     */
    private String describe(List<Double> values) {
        return String.format("%.2f / %.2f / %.2f", mean(values), median(values), standardDeviation(values));
    }
}
