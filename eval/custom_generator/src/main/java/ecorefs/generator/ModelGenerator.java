package ecorefs.generator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelGenerator {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Dynamic EMF Model Generator...");

        // 1. Initialize EMF
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        // 2. Define Dynamic Metamodel (Component with ID, Name, and Children)
        EcoreFactory factory = EcoreFactory.eINSTANCE;
        EPackage myPackage = factory.createEPackage();
        myPackage.setName("benchmark");
        myPackage.setNsPrefix("bm");
        myPackage.setNsURI("http://benchmark/1.0");

        EClass componentClass = factory.createEClass();
        componentClass.setName("Component");

        EAttribute idAttr = factory.createEAttribute();
        idAttr.setName("id");
        idAttr.setEType(EcorePackage.eINSTANCE.getEInt());
        componentClass.getEStructuralFeatures().add(idAttr);

        EReference childrenRef = factory.createEReference();
        childrenRef.setName("children");
        childrenRef.setEType(componentClass);
        childrenRef.setUpperBound(-1);
        childrenRef.setContainment(true);
        componentClass.getEStructuralFeatures().add(childrenRef);

        myPackage.getEClassifiers().add(componentClass);
        EPackage.Registry.INSTANCE.put(myPackage.getNsURI(), myPackage);

        // 3. Generate Models of Increasing Sizes
        int[] scales = {100, 1000, 10000, 100000, 500000};
        File outDir = new File("output");
        outDir.mkdirs();

        for (int scale : scales) {
            System.out.print("Generating model with " + scale + " elements... ");
            ResourceSet resourceSet = new ResourceSetImpl();
            Resource resource = resourceSet.createResource(URI.createFileURI(new File(outDir, "model_" + scale + ".xmi").getAbsolutePath()));

            // Generate tree
            EObject root = myPackage.getEFactoryInstance().create(componentClass);
            root.eSet(idAttr, 0);
            resource.getContents().add(root);

            List<EObject> previousLevel = new ArrayList<>();
            previousLevel.add(root);
            
            int idCounter = 1;
            while (idCounter < scale) {
                List<EObject> currentLevel = new ArrayList<>();
                for (EObject parent : previousLevel) {
                    // add 2 children per node (binary tree) until target scale reached
                    for (int i = 0; i < 2 && idCounter < scale; i++) {
                        EObject child = myPackage.getEFactoryInstance().create(componentClass);
                        child.eSet(idAttr, idCounter++);
                        @SuppressWarnings("unchecked")
                        List<EObject> childrenList = (List<EObject>) parent.eGet(childrenRef);
                        childrenList.add(child);
                        currentLevel.add(child);
                    }
                    if (idCounter >= scale) break;
                }
                previousLevel = currentLevel;
            }

            resource.save(Collections.emptyMap());
            long fileSizeBytes = new File(outDir, "model_" + scale + ".xmi").length();
            double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            System.out.printf("Saved: model_%d.xmi (%.2f MB)%n", scale, fileSizeMB);
        }
        
        System.out.println("All benchmarking models successfully generated.");
    }
}
