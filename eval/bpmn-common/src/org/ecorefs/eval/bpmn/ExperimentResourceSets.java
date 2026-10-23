package org.ecorefs.eval.bpmn;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ecorefs.ipfs.IPFSResourceFactoryImpl;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import io.ipfs.api.IPFS;

/**
 * Builds the resource sets of the experiment. Every set reads IPFS only through
 * the node of the experiment, reads and writes files only inside the dataset and
 * the temporary folder, and refuses every other location. The sets differ in the
 * resource factory: the BPMN 2.0 resource of Eclipse, the plain XMI resource, or
 * the IPFS resource of the core plugin.
 */
public final class ExperimentResourceSets {

    private static final String IPFS_SCHEME = "ipfs";

    private final IPFS ipfs;
    private final List<Path> allowedFolders;

    /**
     * Creates the builder.
     *
     * @param ipfs           the client of the Kubo node of the experiment
     * @param allowedFolders the folders whose files the experiment may read and write
     */
    public ExperimentResourceSets(IPFS ipfs, List<Path> allowedFolders) {
        this.ipfs = ipfs;
        this.allowedFolders = allowedFolders;
    }

    /**
     * Returns the load options for a model from the dataset. The options switch off
     * external DTDs and entities, as the load check of the dataset does, because
     * some test files of the engines declare entities that name local files.
     *
     * @param extendedMetadata whether the XMI load uses extended metadata
     * @return a new map of load options
     */
    public static Map<Object, Object> loadOptions(boolean extendedMetadata) {
        Map<String, Boolean> parserFeatures = new HashMap<>();
        parserFeatures.put("http://xml.org/sax/features/external-general-entities", Boolean.FALSE);
        parserFeatures.put("http://xml.org/sax/features/external-parameter-entities", Boolean.FALSE);
        parserFeatures.put("http://apache.org/xml/features/nonvalidating/load-external-dtd", Boolean.FALSE);
        Map<Object, Object> options = new HashMap<>();
        options.put(XMLResource.OPTION_PARSER_FEATURES, parserFeatures);
        if (extendedMetadata) {
            options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        }
        return options;
    }

    /**
     * Returns the save options of an XMI save.
     *
     * @param extendedMetadata whether the save uses extended metadata
     * @return the options, or null for a plain save, which is how
     *         {@code IPFSResourceImpl} saves by default
     */
    public static Map<Object, Object> xmiSaveOptions(boolean extendedMetadata) {
        if (!extendedMetadata) {
            return null;
        }
        Map<Object, Object> options = new HashMap<>();
        options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        return options;
    }

    /**
     * Creates a resource set that reads and writes BPMN 2.0 XML files.
     *
     * @return the new resource set
     */
    public ResourceSet forBpmnFiles() {
        return newResourceSet(new Bpmn2ResourceFactoryImpl());
    }

    /**
     * Creates a resource set that reads and writes plain XMI files.
     *
     * @return the new resource set
     */
    public ResourceSet forXmiFiles() {
        return newResourceSet(new XMIResourceFactoryImpl());
    }

    /**
     * Creates a resource set whose {@code ipfs} URIs get the IPFS resource of the
     * core plugin, which stores XMI.
     *
     * @return the new resource set
     */
    public ResourceSet forIpfsXmi() {
        ResourceSet resourceSet = newResourceSet(new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put(IPFS_SCHEME, new IPFSResourceFactoryImpl(ipfs));
        return resourceSet;
    }

    /**
     * Creates a resource set whose {@code ipfs} URIs get the BPMN 2.0 resource of
     * Eclipse, so BPMN 2.0 XML published on IPFS loads by CID.
     *
     * @return the new resource set
     */
    public ResourceSet forIpfsBpmnXml() {
        ResourceSet resourceSet = newResourceSet(new Bpmn2ResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put(IPFS_SCHEME, new Bpmn2ResourceFactoryImpl());
        return resourceSet;
    }

    /**
     * Creates a resource set with the given default factory and the restricted
     * URI handlers of the experiment.
     *
     * @param defaultFactory the factory for every file extension
     * @return the new resource set
     */
    private ResourceSet newResourceSet(Resource.Factory defaultFactory) {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, defaultFactory);
        EList<URIHandler> handlers = resourceSet.getURIConverter().getURIHandlers();
        handlers.clear();
        handlers.add(new IpfsOnlyURIHandler(ipfs));
        handlers.add(new ExperimentFileURIHandler(allowedFolders));
        handlers.add(new DeniedURIHandler());
        return resourceSet;
    }
}
