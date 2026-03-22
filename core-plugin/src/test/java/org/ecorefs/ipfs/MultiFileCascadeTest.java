package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class MultiFileCascadeTest {

    private static IPFS ipfs;
    private static ResourceSet ipfsResourceSet;
    private static EPackage dynamicPackage;
    private static EClass componentClass;
    private static EReference childrenRef;

    @BeforeAll
    public static void setup() {
        ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

        ipfsResourceSet = new ResourceSetImpl();
        IPFSResourceFactoryImpl factory_ipfs = new IPFSResourceFactoryImpl(ipfs);
        ipfsResourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipfs", factory_ipfs);
        ipfsResourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipns", factory_ipfs);
        ipfsResourceSet.getURIConverter().getURIHandlers()
                .add(0, new IPFSURIHandlerImpl(ipfs));

        EcoreFactory factory = EcoreFactory.eINSTANCE;
        dynamicPackage = factory.createEPackage();
        dynamicPackage.setName("cascade");
        dynamicPackage.setNsURI("http://cascade/1.0");
        dynamicPackage.setNsPrefix("cas");

        componentClass = factory.createEClass();
        componentClass.setName("Node");

        EAttribute idAttr = factory.createEAttribute();
        idAttr.setName("id");
        idAttr.setEType(EcorePackage.eINSTANCE.getEString());
        componentClass.getEStructuralFeatures().add(idAttr);

        childrenRef = factory.createEReference();
        childrenRef.setName("references");
        childrenRef.setEType(componentClass);
        childrenRef.setUpperBound(-1);
        childrenRef.setContainment(false); 
        componentClass.getEStructuralFeatures().add(childrenRef);

        dynamicPackage.getEClassifiers().add(componentClass);
        EPackage.Registry.INSTANCE.put(dynamicPackage.getNsURI(), dynamicPackage);
    }

    private static double calculateSD(long[] numArray) {
        double sum = 0.0, std = 0.0;
        for(double num : numArray) sum += num;
        double mean = sum / numArray.length;
        for(double num: numArray) std += Math.pow(num - mean, 2);
        return Math.sqrt(std / numArray.length);
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

    @SuppressWarnings("unchecked")
    @Test
    public void testCascadeSave() throws Exception {
        int warmupIterations = 3;
        int measuredIterations = 10;
        long[] cascadeSaves = new long[measuredIterations];

        Files.createDirectories(Paths.get("/data2/projects/ecorefs/eval/results"));
        File csvFile = new File("/data2/projects/ecorefs/eval/results/cascade_metrics.csv");
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            writer.println("Iteration,IpfsCascadeSave(ms)");

            for (int iter = 0; iter < warmupIterations + measuredIterations; iter++) {
                boolean isWarmup = iter < warmupIterations;
                
                ipfsResourceSet.getResources().clear();
                
                Resource resA = ipfsResourceSet.createResource(URI.createURI("ipfs://pendingA"));
                Resource resB = ipfsResourceSet.createResource(URI.createURI("ipfs://pendingB"));

                EObject rootNode = dynamicPackage.getEFactoryInstance().create(componentClass);
                rootNode.eSet(componentClass.getEStructuralFeature("id"), "RootNode_" + iter);
                resA.getContents().add(rootNode);

                EObject childNode = dynamicPackage.getEFactoryInstance().create(componentClass);
                childNode.eSet(componentClass.getEStructuralFeature("id"), "ChildNode_" + iter);
                resB.getContents().add(childNode);

                ((java.util.List<EObject>) rootNode.eGet(childrenRef)).add(childNode);

                long t0 = System.nanoTime();
                IPFSModelPersister.cascadeSave(resB, Collections.emptyMap());
                long tCascade = (System.nanoTime() - t0) / 1_000_000;

                if (!isWarmup) {
                    int index = iter - warmupIterations;
                    cascadeSaves[index] = tCascade;
                    writer.printf("%d,%d\n", index+1, tCascade);
                }
            }
            
            System.out.println("================================");
            System.out.println("Cascade Benchmark Multi-Iteration");
            System.out.printf("Mean: %d ms, Median: %d ms, SD: %.2f ms\n", 
                calculateMean(cascadeSaves), calculateMedian(cascadeSaves), calculateSD(cascadeSaves));
            System.out.println("================================");
        }
    }
}
