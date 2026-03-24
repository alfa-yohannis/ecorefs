/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.examples.customtask;

import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.ui.AbstractBpmn2RuntimeExtension;
import org.eclipse.bpmn2.modeler.ui.wizards.FileService;
import org.eclipse.ui.IEditorInput;
import org.xml.sax.InputSource;

public class MyRuntimeExtension extends AbstractBpmn2RuntimeExtension {

	public final static String TARGET_NAMESPACE = "http://org.eclipse.bpmn2.modeler.examples.customtask";
	public MyRuntimeExtension() {
	}

	@Override
	public boolean isContentForRuntime(IEditorInput input) {
		  InputSource source = new InputSource( FileService.getInputContents(input) );
		  RootElementParser parser = new RootElementParser(TARGET_NAMESPACE);
		  parser.parse(source);
		  return parser.getResult();
	}

	@Override
	public String getTargetNamespace(Bpmn2DiagramType diagramType) {
		return TARGET_NAMESPACE;
	}
}
