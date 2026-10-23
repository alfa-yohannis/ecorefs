package org.ecorefs.eval.baselines;

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

import java.nio.file.Path;
import java.util.Collection;

/**
 * The generated models of the RQ3 benchmark of the paper. The metamodel has one
 * class, {@code Component}, with an integer identifier and contained children,
 * and it is created at run time exactly as in {@code FastPerformanceBenchmarkTest}
 * of the core plug-in, so the models load without generated Java code.
 */
public final class BenchmarkModels {

    /** Namespace of the benchmark metamodel. */
    public static final String NAMESPACE = "http://benchmark/1.0";

    /** Name of the identifier attribute that every iteration stamps. */
    private static final String IDENTIFIER = "id";

    /** The registered benchmark metamodel. */
    private final EPackage benchmarkPackage;

    /**
     * Creates the benchmark metamodel and registers the metamodel in the global
     * package registry.
     */
    public BenchmarkModels() {
        benchmarkPackage = createPackage(NAMESPACE, "benchmark");
        EPackage.Registry.INSTANCE.put(NAMESPACE, benchmarkPackage);
    }

    /**
     * Creates a package with the benchmark class. The NeoEMF store builds a second
     * package with the same structure under another namespace.
     *
     * @param namespace the namespace URI of the new package
     * @param name      the name of the new package
     * @return the new, unregistered package
     */
    public static EPackage createPackage(String namespace, String name) {
        EcoreFactory factory = EcoreFactory.eINSTANCE;
        EPackage newPackage = factory.createEPackage();
        newPackage.setName(name);
        newPackage.setNsPrefix("bm");
        newPackage.setNsURI(namespace);

        EClass componentClass = factory.createEClass();
        componentClass.setName("Component");

        EAttribute identifierAttribute = factory.createEAttribute();
        identifierAttribute.setName(IDENTIFIER);
        identifierAttribute.setEType(EcorePackage.eINSTANCE.getEInt());
        componentClass.getEStructuralFeatures().add(identifierAttribute);

        EReference childrenReference = factory.createEReference();
        childrenReference.setName("children");
        childrenReference.setEType(componentClass);
        childrenReference.setUpperBound(-1);
        childrenReference.setContainment(true);
        componentClass.getEStructuralFeatures().add(childrenReference);

        newPackage.getEClassifiers().add(componentClass);
        return newPackage;
    }

    /**
     * Builds a resource set that reads and writes XMI files with the default
     * options, as the local XMI path of the paper does.
     *
     * @return a resource set with the XMI factory for every extension
     */
    public static ResourceSet createXmiResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        return resourceSet;
    }

    /**
     * Loads the generated model of one size from the generator output.
     *
     * @param modelDirectory folder with the files {@code model_<elements>.xmi}
     * @param elementCount   number of elements of the wanted model
     * @return the loaded resource
     */
    public Resource loadSource(Path modelDirectory, int elementCount) {
        Path file = modelDirectory.resolve("model_" + elementCount + ".xmi");
        return createXmiResourceSet().getResource(URI.createFileURI(file.toString()), true);
    }

    /**
     * Copies the source model and stamps the copy with the iteration number, so
     * every iteration stores different bytes.
     *
     * @param source    the loaded source model
     * @param iteration number of the current iteration
     * @return the root objects of the copy
     */
    public Collection<EObject> copyForIteration(Resource source, int iteration) {
        Collection<EObject> copy = EcoreUtil.copyAll(source.getContents());
        for (EObject root : copy) {
            stampIdentifier(root, iteration);
        }
        return copy;
    }

    /**
     * Writes the iteration number into the identifier of a root object.
     *
     * @param root      the root object of a copy
     * @param iteration number of the current iteration
     */
    private static void stampIdentifier(EObject root, int iteration) {
        EStructuralFeature identifier = root.eClass().getEStructuralFeature(IDENTIFIER);
        if (identifier != null) {
            root.eSet(identifier, iteration);
        }
    }

    /**
     * Returns the registered benchmark metamodel.
     *
     * @return the package with the namespace {@link #NAMESPACE}
     */
    public EPackage benchmarkPackage() {
        return benchmarkPackage;
    }
}
