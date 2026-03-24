package org.eclipse.bpmn2.modeler.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public class MarkerUtils {
    
    public static List<EObject> getAllObjects(ResourceSet resourceSet, IMarker marker) {
    	List<EObject> result = new ArrayList<EObject>();
    	result.add(getTargetObject(resourceSet, marker));
    	result.addAll(getRelatedObjects(resourceSet, marker));
    	return result;
    }

    public static List<EObject> getRelatedObjects(ResourceSet resourceSet, IMarker marker) {
    	List<EObject> result = new ArrayList<EObject>();
    	String targetUri = marker.getAttribute(EValidator.URI_ATTRIBUTE, null);
    	String uriString = marker.getAttribute(EValidator.RELATED_URIS_ATTRIBUTE,null);
    	if (uriString!=null) {
    		String[] uris = uriString.split(" "); //$NON-NLS-1$
    		for (String s : uris) {
    			if (s.equals(targetUri))
    				continue;
    	        URI uri = URI.createURI(s);
    	        EObject o = resourceSet.getEObject(uri, false);
    	        if (!(o instanceof EStructuralFeature))
    	        	result.add(o);
    		}
    	}
    	return result;
    }
    
    public static EObject getTargetObject(ResourceSet resourceSet, IMarker marker) {
        final String uriString = marker.getAttribute(EValidator.URI_ATTRIBUTE, null);
        final URI uri = uriString == null ? null : URI.createURI(uriString);
        if (uri == null) {
            return null;
        }
        return resourceSet.getEObject(uri, false);
    }

    public static ContainerShape getContainerShape(IFeatureProvider fp, IMarker marker) {
    	ResourceSet rs = fp.getDiagramTypeProvider().getDiagramBehavior().getEditingDomain().getResourceSet();
    	for (EObject o : getAllObjects(rs, marker)) {
    		for (PictogramElement pe : fp.getAllPictogramElementsForBusinessObject(o)) {
    			if (pe instanceof ContainerShape)
    				return (ContainerShape)pe;
    		}
    	}
    	return null;
    }
}
