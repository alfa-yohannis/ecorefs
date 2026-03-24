/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.core.model;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.di.BpmnDiFactory;
import org.eclipse.bpmn2.di.BpmnDiPackage;
import org.eclipse.bpmn2.impl.Bpmn2FactoryImpl;
import org.eclipse.bpmn2.impl.DocumentRootImpl;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.runtime.CustomTaskDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.ModelExtensionDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntimeAdapter;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.core.runtime.Assert;
import org.eclipse.dd.dc.DcPackage;
import org.eclipse.dd.di.DiPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.osgi.util.NLS;

/**
 * This Factory will invoke the super factory to create a "bare bones"
 * model object, and then "decorate" it with model extensions defined
 * by the Target Runtime plugin extension.
 *
 * Note the create(EClass) and create(Resource,EClass) methods override default Bpmn2Factory behavior by
 * decorating newly constructed objects with optional artifacts as defined by the Target Runtime extension
 * plugin. These also add adapters to the new EObjects to help identify the Target Runtime and Resource (among other things).
 * 
 * The createObject(Resource,...) and createFeature(EObject,EStructuralFeature,...) methods route object creation through
 * the ExtensionPropertiesAdapter attached to the EObject. This allows Target Runtime extension plugins to override object
 * creation. These methods should eventually invoke this factory's create(Resource,EClass) method.
 *  
 * @author Bob Brodt
 *
 */
public class Bpmn2ModelerFactory extends Bpmn2FactoryImpl {

	public static final String TARGET_RUNTIME = "TargetRuntime";
	public static final String INITIALIZE_OBJECT = "InitializeObject";
	
	/**
	 * A simple class for passing name/value pairs to create() method.
	 */
	public static class KeyValue implements java.util.Map.Entry<String,Object> {
		private String key;
		private Object value;
		
		public KeyValue(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Object setValue(Object value) {
			this.value = value;
			return value;
		}
	}

	/**
	 * We provide our own DocumentRoot (since we can't modify the one in org.eclipse.bpmn2)
	 * which prevents forwarding change notifications to the XML Namespace Prefix map AFTER
	 * the document has been saved. This avoids the nasty "Cannot modify resource set without
	 * a write transaction" error.
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=392427
	 */
	public class Bpmn2ModelerDocumentRootImpl extends DocumentRootImpl {
		
		private boolean deliver = true;
		
		public Bpmn2ModelerDocumentRootImpl() {
			super();
		}
		
		public void setDeliver(boolean deliver) {
			this.deliver = deliver;
		}
	    public Map<String, String> getXMLNSPrefixMap() {
	        if (xMLNSPrefixMap == null) {
	            xMLNSPrefixMap = new EcoreEMap<String, String>(
	                    EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY,
	                    EStringToStringMapEntryImpl.class, this,
	                    Bpmn2Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP) {
	                {
	                    initializeDelegateEList();
	                }

	                @Override
	                protected void initializeDelegateEList() {
	                    delegateEList = new DelegateEObjectContainmentEList<Entry<String, String>>(entryClass,
	                    		Bpmn2ModelerDocumentRootImpl.this, Bpmn2Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP) {
	                        @Override
	                        protected void dispatchNotification(Notification notification) {
	                            if (deliver)
	                            	super.dispatchNotification(notification);
	                        }
	                    };
	                }
	            };
	            // To remove the "bpmn2:" namespace prefix from all elements, just add this
	            // as the default namespace to the <definitions> namespace prefix map
	            // xMLNSPrefixMap.map().put("", Bpmn2Package.eNS_URI);
	        }
	        return xMLNSPrefixMap.map();

	    }
	}
	
	// Allows the XML loader for a particular target runtime to temporarily disable
	// model extensions. This prevents extensions being added multiple times by
	// ModelExtensionDescriptor.populateObject() every time a file is loaded.
	protected static boolean enableModelExtensions = true;
	protected static Resource resource;
	private static ReentrantLock lock = new ReentrantLock();
	
	public static void lock() {
		lock.lock();
	}
	
	public static void unlock() {
		lock.unlock();
	}

	public static Bpmn2Factory getInstance() {
		return Bpmn2ModelerFactory.eINSTANCE;
	}
	
	@Override
    public DocumentRoot createDocumentRoot() {
        DocumentRootImpl documentRoot = new Bpmn2ModelerDocumentRootImpl();
        return documentRoot;
    }

	@Override
    public EObject create(EClass eClass) {
		if (resource!=null) {
			return create(resource, eClass);
		}
		return super.create(eClass);
    }

	public EObject createInternal(EClass eClass) {
		return super.create(eClass);
	}
	
	// convenience methods
	public static <T extends EObject> T create(Resource resource, Class<T> clazz) {
		EClass eClass = getEClass(resource, clazz);
		return (T) create(resource, eClass);
	}

	public static EObject create(Resource resource, EClass eClass, KeyValue... args) {
		Map<String, Object> map = new Hashtable<String, Object>();
		if (args!=null) {
			for (KeyValue kv : args) {
				if (kv.getValue()!=null) 
					map.put(kv.getKey(), kv.getValue());
			}
		}
		return create(resource, eClass, map);
	}
	
	public static EObject create(Resource resource, EClass eClass, Map<String,Object> args) {
		Assert.isTrue(eClass!=null);
		Assert.isTrue(resource!=null);
		
		EObject object = null;
		lock();
		try {
    		String customElementId = null;
			TargetRuntime rt = null;
			boolean initializeObject = enableModelExtensions;

			if (args!=null) {
				for (java.util.Map.Entry<String, Object> kv : args.entrySet()) {
					if (GraphitiConstants.CUSTOM_ELEMENT_ID.equals(kv.getKey()))
						customElementId = (String)kv.getValue();
					if (TARGET_RUNTIME.equals(kv.getKey()))
						rt = (TargetRuntime) kv.getValue();
					if (INITIALIZE_OBJECT.equals(kv.getKey()))
						initializeObject = (Boolean) kv.getValue();
				}
			}

			if (rt==null)
				rt = TargetRuntime.getRuntime(resource);
			Assert.isTrue(rt!=null);

			EFactory factory = eClass.getEPackage().getEFactoryInstance();
			if (factory instanceof Bpmn2ModelerFactory)
				object = ((Bpmn2ModelerFactory) factory).createInternal(eClass);
			else
				object = factory.create(eClass);
			
    		TargetRuntimeAdapter.adapt(object, rt);

			String className = eClass.getName();
    		if (!className.equals(Bpmn2Package.eINSTANCE.getDocumentRoot().getName()) && 
    			rt.getModelDescriptor().getEPackage() != Bpmn2Package.eINSTANCE &&
    			rt.getModelDescriptor().getEPackage() != null &&
    			rt.getModelDescriptor().getEPackage().getEClassifier(className) != null ) {
				EClass clazz = (EClass) rt.getModelDescriptor().getEPackage().getEClassifier(className);
    			object = rt.getModelDescriptor().getEFactory().create(clazz);
			}
    		
    		// first look for Model Extension Descriptors for this specific object type
    		if (customElementId!=null) {
    			CustomTaskDescriptor ctd = rt.getCustomTask(customElementId);
    			if (ctd!=null)
    				ctd.populateObject(object, resource, initializeObject);
    		}
    		else {
	    		List<ModelExtensionDescriptor> list = rt.getModelExtensionDescriptors();
		    	for (ModelExtensionDescriptor med : list) {
		    		if (className.equals(med.getType())) {
		    			med.populateObject(object, resource, initializeObject);
		    		}
		    	}
		    	// then check if there are any MEDs for any supertypes of this object type
		    	for (ModelExtensionDescriptor med : list) {
		    		for (EClass st : eClass.getEAllSuperTypes()) {
			    		if (st.getName().equals(med.getType())) {
			    			med.populateObject(object, resource, initializeObject);
			    		}
		    		}
		    	}
    		}
    		
    		// TODO: should we set a default ID and "name" here?

    		if (initializeObject) {
	    		String id = ModelUtil.setID(object,resource);
	    		// also set a default name
	    		EStructuralFeature feature = object.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
	    		if (feature!=null && !object.eIsSet(feature)) {
	    			if (id!=null)
	    				object.eSet(feature, ModelUtil.toCanonicalString(id));
	    			else {
	    				String name = ModelUtil.toCanonicalString(object.eClass().getName());
	    				object.eSet(feature, NLS.bind(Messages.Bpmn2ModelerFactory_New_Name, name));
	    			}
	    		}
    		}
    		
    		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
    		if (adapter!=null)
    			adapter.setResource(resource);

    		rt.notify(new LifecycleEvent(EventType.BUSINESSOBJECT_CREATED, object, rt));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			unlock();
		}
    	return object;
	}
	
    public static void setEnableModelExtensions(boolean enable) {
    	enableModelExtensions = enable;
    }

    public static boolean getEnableModelExtensions() {
    	return enableModelExtensions;
    }
	
	
	@SuppressWarnings("unchecked")
	public static <T extends EObject> T createObject(Resource resource, Class<T> clazz) {
		EClass eClass = getEClass(resource, clazz);
		T newObject = null;
		if (eClass!=null) {
			newObject = (T) createObject(resource, eClass);
		}
		else {
			// maybe it's a DI object type?
			EClassifier eClassifier = BpmnDiPackage.eINSTANCE.getEClassifier(clazz.getSimpleName());
			if (eClassifier instanceof EClass) {
				newObject = (T) BpmnDiFactory.eINSTANCE.create((EClass)eClassifier);
			}
		}
		return newObject;
	}
	
	public static EObject createObject(Resource resource, EClass eClass, KeyValue... args) {
		Map<String, Object> map = new Hashtable<String, Object>();
		if (args!=null) {
			for (KeyValue kv : args) {
				if (kv.getValue()!=null) 
					map.put(kv.getKey(), kv.getValue());
			}
		}
		return createObject(resource, eClass, map);
	}
	
	public static EObject createObject(Resource resource, EClass eClass, Map<String, Object> args) {
		Assert.isTrue(eClass!=null);
		
		EObject newObject = null;
		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(resource, eClass);
		if (adapter!=null) {
			newObject = adapter.getObjectDescriptor().createObject(resource, eClass, args);
		}
		else {
			// There is no properties adapter registered for this class. This can only happen if the object to
			// be created is in an external package. If this is the case, simply construct an object using the
			// registered model factory.
			EPackage pkg = eClass.getEPackage();
			if (!isBpmnPackage(pkg)) {
				newObject = pkg.getEFactoryInstance().create(eClass);
			}
		}
		if (newObject!=null) {
			TargetRuntime rt = TargetRuntime.getRuntime(newObject);
			rt.notify(new LifecycleEvent(EventType.BUSINESSOBJECT_CREATED, newObject, rt));
		}
		return newObject;
	}
	
	public static void setResource(Resource r) {
		resource = r;
	}
	
//	public static EObject createFeature(EObject object, EStructuralFeature feature) {
//		return createFeature(object.eResource(), object, feature, (Class<? extends EObject>)feature.getEType().getInstanceClass());
//	}
//	public static EObject createFeature(EObject object, String featureName) {
//		return createFeature(object, object.eClass().getEStructuralFeature(featureName));
//	}
//
//	//
//	
//	public static <T extends EObject> T createFeature(EObject object, EStructuralFeature feature, Class<T> clazz) {
//		return createFeature(object.eResource(), object, feature, clazz);
//	}
//	public static <T extends EObject> T createFeature(EObject object, String featureName, Class<T> clazz) {
//		return createFeature(object, object.eClass().getEStructuralFeature(featureName), clazz);
//	}
//	
//	//
//	
//	public static EObject createFeature(EObject object, EStructuralFeature feature, EClass eClass) {
//		return createFeature(object.eResource(), object, feature, eClass);
//	}
//	public static EObject createFeature(EObject object, String featureName, EClass eClass) {
//		return createFeature(object.eResource(), object, object.eClass().getEStructuralFeature(featureName), eClass);
//	}
//	
	
	public static <T extends EObject> T createFeature(Resource resource, EObject object, String featureName, Class<T> clazz) {
		return createFeature(resource, object, object.eClass().getEStructuralFeature(featureName), clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T extends EObject> T createFeature(Resource resource, EObject object, EStructuralFeature feature, Class<T> clazz) {
		Assert.isTrue(feature.getEType().getInstanceClass().isAssignableFrom(clazz));
		
		EClass eClass = getEClass(resource, clazz);
		return (T)createFeature(resource, object, feature, eClass);
	}

	public static EObject createFeature(Resource resource, EObject object, EStructuralFeature feature, EClass eClass) {
		if (eClass==null)
			eClass = (EClass)feature.getEType();
		Assert.isTrue(feature.getEType().getInstanceClass().isAssignableFrom( eClass.getInstanceClass() ));

		EObject newObject = null;

		if (resource==null)
			resource = object.eResource();
		
		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
		if (adapter!=null) {
			newObject = adapter.getFeatureDescriptor(feature).createFeature(resource, eClass);
		}
		else {
			// There is no properties adapter registered for this class. This can only happen if the object to
			// be created is in an external package. If this is the case, simply construct an object using the
			// registered model factory.
			EPackage pkg = eClass.getEPackage();
			if (!isBpmnPackage(pkg)) {
				newObject = pkg.getEFactoryInstance().create(eClass);
			}
		}
		if (newObject!=null) {
			TargetRuntime rt = TargetRuntime.getRuntime(newObject);
			rt.notify(new LifecycleEvent(EventType.BUSINESSOBJECT_CREATED, newObject, rt));
		}
		return newObject;
	}

	private static EClass getEClass(Resource resource, Class clazz) {
		String nsURI = null;
		if (resource!=null) {
			TargetRuntime rt = TargetRuntimeAdapter.getTargetRuntime(resource);
			if (rt!=null)
				nsURI = rt.getRuntimeExtension().getTargetNamespace(null);
		}
		EPackage pkg = ModelDecorator.getEPackage(nsURI);
		EClassifier eClassifier = ModelDecorator.findEClassifier(pkg, clazz.getSimpleName());
		if (eClassifier instanceof EClass) {
			return (EClass)eClassifier;
		}
		return null;
	}

	public static boolean isBpmnPackage(EPackage pkg) {
		return	pkg == Bpmn2Package.eINSTANCE ||
				pkg == BpmnDiPackage.eINSTANCE ||
				pkg == DcPackage.eINSTANCE ||
				pkg == DiPackage.eINSTANCE;
	}

	public static boolean isBpmnPackage(String nsURI) {
		if (nsURI==null || nsURI.isEmpty())
			return true;
		
		if (!nsURI.endsWith("-XMI")) //$NON-NLS-1$
			nsURI += "-XMI"; //$NON-NLS-1$
		return	Bpmn2Package.eINSTANCE.getNsURI().equals(nsURI) ||
				BpmnDiPackage.eINSTANCE.getNsURI().equals(nsURI) ||
				DcPackage.eINSTANCE.getNsURI().equals(nsURI) ||
				DiPackage.eINSTANCE.getNsURI().equals(nsURI);
	}
}
