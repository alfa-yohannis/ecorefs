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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class PerformanceBenchmarkTest {

    private static IPFS ipfs;
    private static ResourceSet nativeResourceSet;
    private static ResourceSet ipfsResourceSet;

    @BeforeAll
    public static void setup() {
        // Init local IPFS connection to the Docker Kubo node
        ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

        // Init traditional EMF file-based ResourceSet
        nativeResourceSet = new ResourceSetImpl();
        nativeResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        // Init our custom IPFS EMF ResourceSet extension
        ipfsResourceSet = new ResourceSetImpl();
        ipfsResourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipfs", new IPFSResourceFactoryImpl(ipfs));
        ipfsResourceSet.getURIConverter().getURIHandlers()
                .add(0, new IPFSURIHandlerImpl(ipfs));
    }

    @Test
    public void runOverheadBenchmark() throws Exception {
        int[] scales = { 100, 1000, 10000, 100000, 500000 };
        String sourceDir = "/data2/projects/ecorefs/eval/custom_generator/output/";

        System.out.println("==========================================================================");
        System.out.println("ElementCount\tNativeSave(ms)\tIpfsSave(ms)\tNativeLoad(ms)\tIpfsLoad(ms)");
        System.out.println("==========================================================================");

        for (int scale : scales) {
            String sourcePath = sourceDir + "model_" + scale + ".xmi";
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                System.out.println("Source missing: " + sourceFile.getAbsolutePath());
                continue;
            }

            // Reference load to hydrate memory objects
            Resource loaderResource = nativeResourceSet.getResource(URI.createFileURI(sourcePath), true);

            // --- NATIVE SAVE BENCHMARK ---
            long startNativeSave = System.currentTimeMillis();
            Path tempNative = Files.createTempFile("native_", ".xmi");
            Resource nativeRes = nativeResourceSet.createResource(URI.createFileURI(tempNative.toString()));
            nativeRes.getContents().addAll(EcoreUtil.copyAll(loaderResource.getContents()));
            nativeRes.save(Collections.emptyMap());
            long timeNativeSave = System.currentTimeMillis() - startNativeSave;

            // --- NATIVE LOAD BENCHMARK ---
            long startNativeLoad = System.currentTimeMillis();
            Resource nativeLoadRes = nativeResourceSet.getResource(URI.createFileURI(tempNative.toString()), true);
            nativeLoadRes.getContents(); // strictly triggers resolving references and reading stream
            long timeNativeLoad = System.currentTimeMillis() - startNativeLoad;

            // --- IPFS SAVE BENCHMARK ---
            long startIpfsSave = System.currentTimeMillis();
            Resource ipfsRes = ipfsResourceSet.createResource(URI.createURI("ipfs://pending"));
            ipfsRes.getContents().addAll(EcoreUtil.copyAll(loaderResource.getContents()));
            ipfsRes.save(Collections.emptyMap());
            long timeIpfsSave = System.currentTimeMillis() - startIpfsSave;
            URI finalIpfsUri = ipfsRes.getURI();

            // --- IPFS LOAD BENCHMARK ---
            long startIpfsLoad = System.currentTimeMillis();
            // Natively resolve the saved CID directly from IPFS network through our
            // IPFSURIHandler
            Resource ipfsLoadRes = ipfsResourceSet.createResource(finalIpfsUri);
            ipfsLoadRes.load(Collections.emptyMap());
            ipfsLoadRes.getContents();
            long timeIpfsLoad = System.currentTimeMillis() - startIpfsLoad;

            System.out.printf("%d\t\t%d\t\t%d\t\t%d\t\t%d\n", scale, timeNativeSave, timeIpfsSave, timeNativeLoad,
                    timeIpfsLoad);

            Files.deleteIfExists(tempNative);
        }
        System.out.println("==========================================================================");
    }
}
