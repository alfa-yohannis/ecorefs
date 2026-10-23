package org.ecorefs.eval.multinode;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Gives access to the generated models of the RQ3 benchmark of the paper, so the
 * multi-node run uses the same content as the local benchmark. The models
 * conform to a metamodel with one class, Component, which the class registers
 * at runtime the same way as FastPerformanceBenchmarkTest of the core plug-in.
 */
public final class GeneratedModels {

    /** Namespace of the generated benchmark metamodel. */
    private static final String BENCHMARK_NAMESPACE = "http://benchmark/1.0";

    /** Folder that holds model_100.xmi to model_500000.xmi. */
    private final Path directory;

    /**
     * Creates the access to the generated models and registers their metamodel.
     *
     * @param directory the folder eval/custom_generator/output
     */
    public GeneratedModels(Path directory) {
        this.directory = directory;
        registerMetamodel();
    }

    /**
     * Returns the file of the model with a given number of elements.
     *
     * @param elements the number of elements, such as 10000
     * @return the path of the XMI file
     */
    public Path file(int elements) {
        return directory.resolve("model_" + elements + ".xmi");
    }

    /**
     * Returns the size of the XMI file of a model.
     *
     * @param elements the number of elements
     * @return the size in bytes
     * @throws IOException when the file is missing
     */
    public long fileSize(int elements) throws IOException {
        return Files.size(file(elements));
    }

    /**
     * Loads a generated model once, so every iteration copies the model from
     * memory instead of reading the file again.
     *
     * @param elements the number of elements
     * @return the loaded resource
     */
    public Resource load(int elements) {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        return resourceSet.getResource(URI.createFileURI(file(elements).toString()), true);
    }

    /**
     * Copies a loaded model and stamps the copy, so the serialized bytes differ
     * from every earlier copy and every save stores new content under a new CID.
     *
     * @param source the loaded model
     * @param stamp  the number written into the identifier of the root objects
     * @return the root objects of the copy
     */
    public Collection<EObject> stampedCopy(Resource source, int stamp) {
        Collection<EObject> copy = EcoreUtil.copyAll(source.getContents());
        for (EObject root : copy) {
            EStructuralFeature identifier = root.eClass().getEStructuralFeature("id");
            if (identifier != null) {
                root.eSet(identifier, stamp);
            }
        }
        return copy;
    }

    /**
     * Creates the benchmark metamodel and puts it into the global package
     * registry, unless an earlier instance did so already.
     */
    private static void registerMetamodel() {
        if (EPackage.Registry.INSTANCE.containsKey(BENCHMARK_NAMESPACE)) {
            return;
        }
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
}
