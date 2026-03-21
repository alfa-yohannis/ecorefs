package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Arrays;

public class FastPerformanceBenchmarkTest {

    private static IPFS ipfs;
    private static ResourceSet nativeResourceSet;
    private static ResourceSet ipfsResourceSet;

    @BeforeAll
    public static void setup() {
        ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

        nativeResourceSet = new ResourceSetImpl();
        nativeResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        ipfsResourceSet = new ResourceSetImpl();
        ipfsResourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipfs", new IPFSResourceFactoryImpl(ipfs));
        ipfsResourceSet.getURIConverter().getURIHandlers()
                .add(0, new IPFSURIHandlerImpl(ipfs));
                
        // Register the dynamic benchmark metamodel
        org.eclipse.emf.ecore.EcoreFactory factory = org.eclipse.emf.ecore.EcoreFactory.eINSTANCE;
        org.eclipse.emf.ecore.EPackage myPackage = factory.createEPackage();
        myPackage.setName("benchmark");
        myPackage.setNsPrefix("bm");
        myPackage.setNsURI("http://benchmark/1.0");

        org.eclipse.emf.ecore.EClass componentClass = factory.createEClass();
        componentClass.setName("Component");

        org.eclipse.emf.ecore.EAttribute idAttr = factory.createEAttribute();
        idAttr.setName("id");
        idAttr.setEType(org.eclipse.emf.ecore.EcorePackage.eINSTANCE.getEInt());
        componentClass.getEStructuralFeatures().add(idAttr);

        org.eclipse.emf.ecore.EReference childrenRef = factory.createEReference();
        childrenRef.setName("children");
        childrenRef.setEType(componentClass);
        childrenRef.setUpperBound(-1);
        childrenRef.setContainment(true);
        componentClass.getEStructuralFeatures().add(childrenRef);

        myPackage.getEClassifiers().add(componentClass);
        org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.put(myPackage.getNsURI(), myPackage);
    }

    private static double calculateSD(long[] numArray) {
        double sum = 0.0, standardDeviation = 0.0;
        for(double num : numArray) sum += num;
        double mean = sum / numArray.length;
        for(double num: numArray) standardDeviation += Math.pow(num - mean, 2);
        return Math.sqrt(standardDeviation / numArray.length);
    }

    private static long calculateMedian(long[] numArray) {
        long[] copy = Arrays.copyOf(numArray, numArray.length);
        Arrays.sort(copy);
        if (copy.length % 2 == 0) return (copy[copy.length/2] + copy[copy.length/2 - 1])/2;
        else return copy[copy.length/2];
    }
    
    private static long calculateMean(long[] numArray) {
        long sum = 0;
        for (long num : numArray) sum += num;
        return sum / numArray.length;
    }

    @Test
    public void runOverheadBenchmark() throws Exception {
        int[] scales = {100, 1000, 10000};
        String sourceDir = "/data2/projects/ecorefs/eval/custom_generator/output/";
        int warmupIterations = 3;
        int measuredIterations = 10;
        
        Files.createDirectories(Paths.get("/data2/projects/ecorefs/eval/results"));
        File csvFile = new File("/data2/projects/ecorefs/eval/results/fast_test_metrics.csv");
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            writer.println("Scale,Iteration,NativeSave(ms),IpfsSave(ms),NativeLoad(ms),IpfsLoad(ms)");

            System.out.println("==========================================================================================================");
            System.out.println("ElementCount\tNativeSave(Mean/Med/SD)\tIpfsSave(Mean/Med/SD)\tNativeLoad(Mean/Med/SD)\tIpfsLoad(Mean/Med/SD)");
            System.out.println("==========================================================================================================");
            
            for (int scale : scales) {
                String sourcePath = sourceDir + "model_" + scale + ".xmi";
                File sourceFile = new File(sourcePath);
                if (!sourceFile.exists()) continue;

                Resource loaderResource = nativeResourceSet.getResource(URI.createFileURI(sourcePath), true);
                
                long[] nativeSaves = new long[measuredIterations];
                long[] ipfsSaves = new long[measuredIterations];
                long[] nativeLoads = new long[measuredIterations];
                long[] ipfsLoads = new long[measuredIterations];

                for (int iter = 0; iter < warmupIterations + measuredIterations; iter++) {
                    boolean isWarmup = iter < warmupIterations;

                    long t0 = System.nanoTime();
                    Path tempNative = Files.createTempFile("native_", ".xmi");
                    Resource nativeRes = nativeResourceSet.createResource(URI.createFileURI(tempNative.toString()));
                    nativeRes.getContents().addAll(EcoreUtil.copyAll(loaderResource.getContents()));
                    nativeRes.save(Collections.emptyMap());
                    long tNativeSave = (System.nanoTime() - t0) / 1_000_000;

                    long t1 = System.nanoTime();
                    Resource nativeLoadRes = nativeResourceSet.getResource(URI.createFileURI(tempNative.toString()), true);
                    nativeLoadRes.getContents(); 
                    long tNativeLoad = (System.nanoTime() - t1) / 1_000_000;

                    long t2 = System.nanoTime();
                    Resource ipfsRes = ipfsResourceSet.createResource(URI.createURI("ipfs://pending"));
                    ipfsRes.getContents().addAll(EcoreUtil.copyAll(loaderResource.getContents()));
                    ipfsRes.save(Collections.emptyMap());
                    long tIpfsSave = (System.nanoTime() - t2) / 1_000_000;
                    URI finalIpfsUri = ipfsRes.getURI();

                    long t3 = System.nanoTime();
                    Resource ipfsLoadRes = ipfsResourceSet.createResource(finalIpfsUri);
                    ipfsLoadRes.load(Collections.emptyMap());
                    ipfsLoadRes.getContents(); 
                    long tIpfsLoad = (System.nanoTime() - t3) / 1_000_000;
                    
                    Files.deleteIfExists(tempNative);

                    if (!isWarmup) {
                        int index = iter - warmupIterations;
                        nativeSaves[index] = tNativeSave;
                        ipfsSaves[index] = tIpfsSave;
                        nativeLoads[index] = tNativeLoad;
                        ipfsLoads[index] = tIpfsLoad;
                        writer.printf("%d,%d,%d,%d,%d,%d\n", scale, index+1, tNativeSave, tIpfsSave, tNativeLoad, tIpfsLoad);
                    }
                }

                System.out.printf("%d\t\t%d/%d/%.2f ms\t\t%d/%d/%.2f ms\t\t%d/%d/%.2f ms\t\t%d/%d/%.2f ms\n", 
                    scale, 
                    calculateMean(nativeSaves), calculateMedian(nativeSaves), calculateSD(nativeSaves),
                    calculateMean(ipfsSaves), calculateMedian(ipfsSaves), calculateSD(ipfsSaves),
                    calculateMean(nativeLoads), calculateMedian(nativeLoads), calculateSD(nativeLoads),
                    calculateMean(ipfsLoads), calculateMedian(ipfsLoads), calculateSD(ipfsLoads));
            }
            System.out.println("==========================================================================================================");
        }
    }
}
