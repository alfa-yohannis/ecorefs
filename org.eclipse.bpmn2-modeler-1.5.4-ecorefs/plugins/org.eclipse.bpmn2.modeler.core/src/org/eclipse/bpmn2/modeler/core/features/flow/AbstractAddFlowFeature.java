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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features.flow;

import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.core.di.DIImport;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.label.AddConnectionLabelFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

public abstract class AbstractAddFlowFeature<T extends BaseElement>
	extends AbstractBpmn2AddFeature<T> {

	public AbstractAddFlowFeature(IFeatureProvider fp) {
		super(fp);
	}

	public IAddFeature getAddLabelFeature(IFeatureProvider fp) {
		return new AddConnectionLabelFeature(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context instanceof IAddConnectionContext) {
			return getBusinessObjectType().isAssignableFrom(getBusinessObject(context).getClass());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.IAdd#add(org.eclipse.graphiti.features.context.IAddContext)
	 */
	@Override
	public PictogramElement add(IAddContext context) {
		boolean isImporting = DIImport.isImporting(context);

		T businessObject = getBusinessObject(context);
		IAddConnectionContext addContext = (IAddConnectionContext) context;

		Anchor sourceAnchor = addContext.getSourceAnchor();
		Anchor targetAnchor = addContext.getTargetAnchor();
		Diagram diagram = Graphiti.getPeService().getDiagramForAnchor(sourceAnchor);
		if (Graphiti.getPeService().getDiagramForAnchor(targetAnchor) != diagram) {
			throw new IllegalArgumentException("Source and Target for "+businessObject.eClass().getName()+" Connection are not in the same Diagram.");
		}
		if (diagram==null)
			diagram = getDiagram();
		
		FreeFormConnection connection = peService.createFreeFormConnection(diagram);
		connection.setStart(sourceAnchor);
		connection.setEnd(targetAnchor);

		createDIEdge(connection, businessObject);
		createConnectionLine(connection);
		
		// create the bendpoints {@see org.eclipse.bpmn2.modeler.core.di.DIImport#createConnectionAndSetBendpoints(BPMNEdge,PictogramElement,PictogramElement)}
		if (isImporting) {
			List<Point> bendpoints = (List<Point>) context.getProperty(GraphitiConstants.CONNECTION_BENDPOINTS);
			if (bendpoints!=null && bendpoints.size()>0) {
				connection.getBendpoints().addAll(bendpoints);
			}
		}
		
		decorateConnection(addContext, connection, businessObject);

		return connection;
	}

	protected Polyline createConnectionLine(Connection connection) {
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(connection);
		Polyline connectionLine = Graphiti.getGaService().createPolyline(connection);
		StyleUtil.applyStyle(connectionLine, be);

		return connectionLine;
	}
}