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
package org.eclipse.bpmn2.modeler.core.di;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.Association;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNLabel;
import org.eclipse.bpmn2.di.BPMNLabelStyle;
import org.eclipse.bpmn2.di.BPMNPlane;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.di.BpmnDiFactory;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.util.Bpmn2Resource;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.dd.dc.DcFactory;
import org.eclipse.dd.dc.Point;
import org.eclipse.dd.di.DiagramElement;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.Font;
import org.eclipse.graphiti.mm.algorithms.styles.Style;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.platform.IDiagramBehavior;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.ILayoutService;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

public class DIUtils {
	
	/**
	 * Creates a BPMNShape if it does not already exist, and then links it to
	 * the given {@code BaseElement}.
	 *
	 * @param shape the Container Shape
	 * @param elem the BaseElement
	 * @param bpmnShape the BPMNShape object. If null, a new one is created and
	 *            inserted into the BPMNDiagram.
	 * @param applyDefaults if true, apply User Preference defaults for certain
	 *            BPMN DI attributes, e.g. isHorizontal, isExpanded, etc.
	 * @return the BPMNShape
	 */
	public static BPMNShape createDIShape(Shape shape, BaseElement elem, BPMNShape bpmnShape, IFeatureProvider fp) {
		if (bpmnShape == null) {
			Diagram diagram = Graphiti.getPeService().getDiagramForShape(shape);
			ILocation loc = Graphiti.getLayoutService().getLocationRelativeToDiagram(shape);
			int x = loc.getX();
			int y = loc.getY();
			int w = shape.getGraphicsAlgorithm().getWidth();
			int h = shape.getGraphicsAlgorithm().getHeight();
			bpmnShape = createDIShape(shape, elem, x, y, w, h, fp, diagram);
		}
		else {
			fp.link(shape, new Object[] { elem, bpmnShape });
		}
		return bpmnShape;
	}

	public static BPMNShape createDIShape(Shape shape, BaseElement elem, int x, int y, int w, int h,
			IFeatureProvider fp, Diagram diagram) {

		EList<EObject> businessObjects = Graphiti.getLinkService().getLinkForPictogramElement(diagram)
				.getBusinessObjects();
		BPMNShape bpmnShape = null;

		for (EObject eObject : businessObjects) {
			if (eObject instanceof BPMNDiagram) {
				BPMNDiagram bpmnDiagram = (BPMNDiagram) eObject;

				bpmnShape = BpmnDiFactory.eINSTANCE.createBPMNShape();
				bpmnShape.setBpmnElement(elem);
				Bounds bounds = DcFactory.eINSTANCE.createBounds();
				bounds.setX(x);
				bounds.setY(y);
				bounds.setWidth(w);
				bounds.setHeight(h);
				bpmnShape.setBounds(bounds);
				getOrCreateDILabel(bpmnShape);

				addDIElement(bpmnShape,bpmnDiagram);
				ModelUtil.setID(bpmnShape);

				fp.link(shape, new Object[] { elem, bpmnShape });
				break;
			}
		}

		return bpmnShape;
	}

	public static void updateDIShape(PictogramElement element) {
		BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(element, BPMNShape.class);
		if (bpmnShape == null) {
			return;
		}

		ILocation loc = Graphiti.getLayoutService().getLocationRelativeToDiagram((Shape) element);
		Bounds bounds = bpmnShape.getBounds();

		bounds.setX(loc.getX());
		bounds.setY(loc.getY());

		GraphicsAlgorithm graphicsAlgorithm = element.getGraphicsAlgorithm();
		IDimension size = Graphiti.getGaService().calculateSize(graphicsAlgorithm);
		bounds.setHeight(size.getHeight());
		bounds.setWidth(size.getWidth());

		if (element instanceof ContainerShape) {
			EList<Shape> children = ((ContainerShape) element).getChildren();
			for (Shape shape : children) {
				if (shape instanceof ContainerShape) {
					updateDIShape(shape);
				}
			}
		}

		updateConnections(element);
	}
	
	private static void updateConnections(PictogramElement element) {
		if (element instanceof Shape) {
			EList<Anchor> anchors = ((Shape) element).getAnchors();
			
			for (Anchor anchor : anchors) {
				List<Connection> connections = Graphiti.getPeService().getAllConnections(anchor);
				for (Connection connection : connections){
					updateDIEdge(connection);
				}
				connections.size();
			}
			
			anchors.size();
		}
	}

	/**
	 * Creates a BPMNEdge if it does not already exist, and then links it to
	 * the given {@code BaseElement}.
	 *
	 * @param connection the connection
	 * @param elem the BaseElement
	 * @param bpmnEdge the BPMNEdge object. If null, a new one is created and
	 *            inserted into the BPMNDiagram.
	 * @return the BPMNEdge
	 */
	public static BPMNEdge createDIEdge(Connection connection, BaseElement elem, BPMNEdge bpmnEdge, IFeatureProvider fp) {
		if (bpmnEdge == null) {
			Diagram diagram = Graphiti.getPeService().getDiagramForPictogramElement(connection);
			EList<EObject> businessObjects = Graphiti.getLinkService().getLinkForPictogramElement(diagram)
					.getBusinessObjects();
			for (EObject eObject : businessObjects) {
				if (eObject instanceof BPMNDiagram) {
					BPMNDiagram bpmnDiagram = (BPMNDiagram) eObject;

					bpmnEdge = BpmnDiFactory.eINSTANCE.createBPMNEdge();
					bpmnEdge.setBpmnElement(elem);

					DiagramElement diSource = BusinessObjectUtil.getFirstElementOfType(connection.getStart().getParent(), DiagramElement.class);
					DiagramElement diTarget = BusinessObjectUtil.getFirstElementOfType(connection.getEnd().getParent(), DiagramElement.class);
					if (diSource==null) {
						if (elem instanceof Association) {
							diSource = DIUtils.findDiagramElement(((Association) elem).getSourceRef());
						} else if (elem instanceof MessageFlow) {
							diSource = DIUtils.findDiagramElement((BaseElement) ((MessageFlow) elem).getSourceRef());
						} else if (elem instanceof SequenceFlow) {
							diSource = DIUtils.findDiagramElement(((SequenceFlow) elem).getSourceRef());
						}
					}
					if (diTarget==null) {
						if (elem instanceof Association) {
							diTarget = DIUtils.findDiagramElement(((Association) elem).getTargetRef());
						} else if (elem instanceof MessageFlow) {
							diTarget = DIUtils.findDiagramElement((BaseElement) ((MessageFlow) elem).getTargetRef());
						} else if (elem instanceof SequenceFlow) {
							diTarget = DIUtils.findDiagramElement(((SequenceFlow) elem).getTargetRef());
						}
					}
					bpmnEdge.setSourceElement(diSource);
					bpmnEdge.setTargetElement(diTarget);
					
					ILocation sourceLoc = Graphiti.getPeService().getLocationRelativeToDiagram(connection.getStart());
					ILocation targetLoc = Graphiti.getPeService().getLocationRelativeToDiagram(connection.getEnd());

					Point point = DcFactory.eINSTANCE.createPoint();
					point.setX(sourceLoc.getX());
					point.setY(sourceLoc.getY());
					bpmnEdge.getWaypoint().add(point);

					point = DcFactory.eINSTANCE.createPoint();
					point.setX(targetLoc.getX());
					point.setY(targetLoc.getY());
					bpmnEdge.getWaypoint().add(point);

					getOrCreateDILabel(bpmnEdge);

					DIUtils.addDIElement(bpmnEdge, bpmnDiagram);
					ModelUtil.setID(bpmnEdge);
				}
			}
		}
		fp.link(connection, new Object[] { elem, bpmnEdge });
		return bpmnEdge;
	}

	public static void updateDIEdge(Connection connection) {
		ILayoutService layoutService = Graphiti.getLayoutService();
		EObject be = BusinessObjectUtil.getFirstElementOfType(connection, BaseElement.class);
		if (be==null)
			return;
		
		BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(connection);
		BPMNEdge edge = DIUtils.findBPMNEdge(bpmnDiagram, be);
		if (edge!=null) {
			Point point = DcFactory.eINSTANCE.createPoint();

			List<Point> waypoint = edge.getWaypoint();
			waypoint.clear();

			ILocation loc;
			loc = layoutService.getLocationRelativeToDiagram(connection.getStart());
			point.setX(loc.getX());
			point.setY(loc.getY());
			waypoint.add(point);

			if (connection instanceof FreeFormConnection) {
				FreeFormConnection freeForm = (FreeFormConnection) connection;
				EList<org.eclipse.graphiti.mm.algorithms.styles.Point> bendpoints = freeForm.getBendpoints();
				for (org.eclipse.graphiti.mm.algorithms.styles.Point bp : bendpoints) {
					point = DcFactory.eINSTANCE.createPoint();
					point.setX(bp.getX());
					point.setY(bp.getY());
					waypoint.add(point);
				}
			}

			point = DcFactory.eINSTANCE.createPoint();
			loc = layoutService.getLocationRelativeToDiagram(connection.getEnd());
			point.setX(loc.getX());
			point.setY(loc.getY());
			waypoint.add(point);
		}
	}

	/**
	 * Add a DiagramElement to a BPMNDiagram container.
	 * 
	 * @param elem the Diagram Element to add
	 * @param bpmnDiagram the BPMNDiagram container to which it is added
	 */
	public static void addDIElement(DiagramElement elem, BPMNDiagram bpmnDiagram) {
		List<DiagramElement> elements = bpmnDiagram.getPlane().getPlaneElement();
		elements.add(elem);
	}
	
	public static BPMNLabel getOrCreateDILabel(DiagramElement de) {
		BPMNLabel bpmnLabel = null;
		EStructuralFeature feature = de.eClass().getEStructuralFeature("label"); //$NON-NLS-1$
		if (feature!=null) {
			bpmnLabel = (BPMNLabel) de.eGet(feature);
			if (bpmnLabel==null) {
				bpmnLabel = BpmnDiFactory.eINSTANCE.createBPMNLabel();
				de.eSet(feature, bpmnLabel);
			}
		}
		return bpmnLabel;
	}
	
	public static void updateDILabel(PictogramElement pe, int x, int y, int w, int h) {
		DiagramElement de = BusinessObjectUtil.getFirstElementOfType(pe, BPMNShape.class);
		if (de==null) {
			de = BusinessObjectUtil.getFirstElementOfType(pe, BPMNEdge.class);
		}
		
		if (de!=null) {
			BPMNLabel bpmnLabel = getOrCreateDILabel(de);
			if (w==0 && h==0) {
				bpmnLabel.setBounds(null);
			}
			else {
				Bounds bounds = bpmnLabel.getBounds();
				if (bounds==null)
					bounds = DcFactory.eINSTANCE.createBounds();
				bounds.setX((float)x);
				bounds.setY((float)y);
				bounds.setWidth((float)w);
				bounds.setHeight((float)h);
				bpmnLabel.setBounds(bounds);
			}
		}
	}
	
	public static BPMNLabelStyle getDILabelStyle(BaseElement element) {
		BPMNLabelStyle bpmnStyle = null;
		DiagramElement de = DIUtils.findDiagramElement(element);
		if (de instanceof BPMNShape) {
			BPMNShape bpmnShape = (BPMNShape) de;
			if (bpmnShape.getLabel() != null) {
				bpmnStyle = bpmnShape.getLabel().getLabelStyle();
			}
		}
		else if (de instanceof BPMNEdge) {
			BPMNEdge bpmnEdge = (BPMNEdge) de;
			if (bpmnEdge.getLabel() != null) {
				bpmnStyle = bpmnEdge.getLabel().getLabelStyle();
			}
		}
		return bpmnStyle;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	public static BPMNLabelStyle getOrCreateDILabelStyle(BaseElement element, ShapeStyle ss) {
		BPMNLabelStyle bpmnStyle = null;
		DiagramElement de = DIUtils.findDiagramElement(element);
		if (de!=null) {
			BPMNLabel bpmnLabel = getOrCreateDILabel(de);
			if (bpmnLabel!=null) {
				BPMNDiagram bpmnDiagram = (BPMNDiagram) de.eContainer().eContainer();
				for (BPMNLabelStyle ls : bpmnDiagram.getLabelStyle()) {
					if (ss.equals(ls)) {
						// found a BPMNLabelStyle that matches the given ShapeStyle
						// so we can reuse it for this BPMNShape or BPMNEdge object.
						bpmnStyle = ls;
					}
				}
				if (bpmnStyle==null) {
					// create a new BPMNLabelStyle and add it to the BPMNDiagram
					bpmnStyle = BpmnDiFactory.eINSTANCE.createBPMNLabelStyle();
					bpmnStyle.setFont( ShapeStyle.toBPMNFont(ss.getLabelFont()) );
					bpmnDiagram.getLabelStyle().add(bpmnStyle);
				}
				bpmnLabel.setLabelStyle(bpmnStyle);
			}
		}		
		return bpmnStyle;
	}
	/* Not currently referenced
	private static boolean compareDILabelStyle(BPMNLabelStyle ls1, BPMNLabelStyle ls2) {
		if (ls1==null) {
			if (ls2==null)
				return true;
			return false;
		}
		if (ls2==null)
			return false;
		org.eclipse.dd.dc.Font f1 = ls1.getFont();
		org.eclipse.dd.dc.Font f2 = ls2.getFont();
		if (f1==null) {
			if (f2==null)
				return true;
			return false;
		}
		if (f2==null)
			return false;
		return f1.getName().equals(f1.getName()) &&
				f1.getSize() == f2.getSize() &&
				f1.isIsBold() == f2.isIsBold() &&
				f1.isIsItalic() == f2.isIsItalic() &&
				f1.isIsStrikeThrough() == f2.isIsStrikeThrough() &&
				f1.isIsUnderline() == f2.isIsUnderline();
	}
	*/
	public static DiagramElement findDiagramElement(List<BPMNDiagram> diagrams, BaseElement bpmnElement) {
		for (BPMNDiagram d : diagrams) {
			BPMNPlane plane = d.getPlane();
			List<DiagramElement> planeElements = plane.getPlaneElement();
			return findPlaneElement(planeElements, bpmnElement);
		}
		return null;
	}

	public static DiagramElement findPlaneElement(List<DiagramElement> planeElements, BaseElement bpmnElement) {
		for (DiagramElement de : planeElements) {
			if (de instanceof BPMNShape) {
				if (bpmnElement == ((BPMNShape)de).getBpmnElement())
					return de;
			}
			if (de instanceof BPMNEdge) {
				if (bpmnElement == ((BPMNEdge)de).getBpmnElement())
					return de;
			}
			else if (de instanceof BPMNPlane) {
				return findPlaneElement(((BPMNPlane)de).getPlaneElement(), bpmnElement);
			}
		}
		return null;
	}

	/**
	 * Return the Graphiti Diagram for the given BPMNDiagram. If one does not exist, create it.
	 * 
	 * @param editor
	 * @param bpmnDiagram
	 * @return
	 */
	public static Diagram getOrCreateDiagram(final IDiagramBehavior editor, final BPMNDiagram bpmnDiagram) {
		// do we need to create a new Diagram or is this already in the model?
		Diagram diagram = findDiagram(editor, bpmnDiagram);
		if (diagram!=null) {
			// already exists
			return diagram;
		}

		// create a new one
		IDiagramTypeProvider dtp = editor.getDiagramContainer().getDiagramTypeProvider();
		final Resource resource = dtp.getDiagram().eResource() != null ?
				dtp.getDiagram().eResource() :
				editor.getEditingDomain().getResourceSet().getResources().get(0);

		final Diagram newDiagram = createDiagram(bpmnDiagram.getName());
		if (newDiagram != null) {
			final IFeatureProvider featureProvider = dtp.getFeatureProvider();
			TransactionalEditingDomain domain = editor.getEditingDomain();
			domain.getCommandStack().execute(new RecordingCommand(domain) {
				protected void doExecute() {
					resource.getContents().add(newDiagram);
					newDiagram.setActive(true);
					featureProvider.link(newDiagram, bpmnDiagram);
				}
			});
		}
		return newDiagram;
	}
	
	public static Diagram createDiagram(String diagramName) {
		final Diagram diagram = Graphiti.getPeCreateService().createDiagram("BPMN2", diagramName, true); //$NON-NLS-1$
		if (diagram != null) {
			Bpmn2Preferences prefs = Bpmn2Preferences.getInstance();
			ShapeStyle ss = prefs.getShapeStyle(ShapeStyle.Category.GRID);
			diagram.setGridUnit(ss.getDefaultWidth());
			diagram.setVerticalGridUnit(ss.getDefaultHeight());
			diagram.setSnapToGrid(ss.getSnapToGrid());
			GraphicsAlgorithm ga = diagram.getGraphicsAlgorithm();
			IGaService gaService = Graphiti.getGaService();
			ga.setForeground(gaService.manageColor(diagram, ss.getShapeForeground()));
			ss = prefs.getShapeStyle(ShapeStyle.Category.CANVAS);
			ga.setBackground(gaService.manageColor(diagram, ss.getShapeBackground()));
		}
		return diagram;
	}
	
	/**
	 * Find the Graphiti Diagram that corresponds to the given BPMNDiagram object.
	 * 
	 * @param editor
	 * @param bpmnDiagram
	 * @return
	 */
	public static Diagram findDiagram(final IDiagramBehavior editor, final BPMNDiagram bpmnDiagram) {
		ResourceSet resourceSet = editor.getEditingDomain().getResourceSet();
		if (resourceSet!=null) {
			return findDiagram(resourceSet, bpmnDiagram);
		}
		return null;
	}
	
	public static Diagram findDiagram(ResourceSet resourceSet, final BPMNDiagram bpmnDiagram) {
		if (resourceSet!=null) {
			for (Resource r : resourceSet.getResources()) {
				for (EObject o : r.getContents()) {
					if (o instanceof Diagram) {
						Diagram diagram = (Diagram)o;
						if (BusinessObjectUtil.getFirstElementOfType(diagram, BPMNDiagram.class) == bpmnDiagram) {
							return diagram;
						}
					}
				}
			}
		}
		return null;
	}
	
	public static void deleteDiagram(final IDiagramBehavior editor, final BPMNDiagram bpmnDiagram) {
		Diagram diagram = DIUtils.findDiagram(editor, bpmnDiagram);
		if (diagram!=null) {
			List<EObject> list = new ArrayList<EObject>();
			TreeIterator<EObject> iter = diagram.eAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o instanceof PictogramLink) {
					((PictogramLink)o).getBusinessObjects().clear();
					if (!list.contains(o))
						list.add(o);
				}
				else if (o instanceof Color) {
					if (!list.contains(o))
						list.add(o);
				}
				else if (o instanceof Font) {
					if (!list.contains(o))
						list.add(o);
				}
				else if (o instanceof Style) {
					if (!list.contains(o))
						list.add(o);
				}
				else if (o instanceof PictogramElement)
					if (!list.contains(o))
						list.add(o);
			}
			for (EObject o : list) {
				if (o instanceof PictogramElement) {
					PictogramLink pl = ((PictogramElement)o).getLink();
					if (pl!=null)
						pl.getBusinessObjects().clear();
					Graphiti.getPeService().deletePictogramElement((PictogramElement)o);
				}
				else
					EcoreUtil.delete(o);
			}
			
			EcoreUtil.delete(diagram);
			EcoreUtil.delete(bpmnDiagram);
		}	
	}
	
	public static BPMNDiagram findBPMNDiagram(PictogramElement pe) {
		Diagram diagram = Graphiti.getPeService().getDiagramForPictogramElement(pe);
		BPMNDiagram bpmnDiagram = BusinessObjectUtil.getFirstElementOfType(diagram, BPMNDiagram.class);
		return bpmnDiagram;
	}
	
	/**
	 * Find the BPMNDiagram in the editor's Resource Set that corresponds to the given BaseElement.
	 * The BaseElement is expected be some kind of container class such as a Process or SubProcess.
	 * 
	 * @param editor
	 * @param baseElement
	 * @return
	 */
	public static BPMNDiagram findBPMNDiagram(final BaseElement baseElement) {
		return findBPMNDiagram(baseElement, false);
	}

	/**
	 * Check if the given BPMNDiagram object contains a BPMNShape or BPMNEdge
	 * that references the given BaseElement.
	 * 
	 * @param bpmnDiagram the BPMNDiagram to check
	 * @param baseElement a BaseElement that will be referenced by a child object of bpmnDiagram
	 * @return true if the baseElement is referenced by a child of bpmnDiagram
	 */
	public static boolean isBPMNDiagramChild(BPMNDiagram bpmnDiagram, BaseElement baseElement) {
		BaseElement bpmnElement;
		for (DiagramElement de : bpmnDiagram.getPlane().getPlaneElement()) {
			if (de instanceof BPMNShape)
				bpmnElement = ((BPMNShape)de).getBpmnElement();
			else if (de instanceof BPMNEdge)
				bpmnElement = ((BPMNEdge)de).getBpmnElement();
			else
				continue;
			if (bpmnElement == baseElement)
				return true;
		}
		return false;
	}
	
	/**
	 * Return a list of all defined BPMNDiagrams. 
	 * 
	 * @param object a reference EObject that belongs to the same ResourceSet as
	 * the Bpmn2Resource being edited.
	 * @param type a flag indicating the types of diagrams to include:
	 *              0 = include all
	 *              1 = include main diagrams (i.e. Collaborations and Processes)
	 *              2 = include sub diagrams (SubProcesses only)
	 * @return a list of BPMNDiagram objects.
	 */
	public static List<BPMNDiagram> getBPMNDiagrams(EObject object, int type) {
		List<BPMNDiagram> result = new ArrayList<BPMNDiagram>();
		if (object==null || object.eResource()==null)
			return result;
		ResourceSet resourceSet = object.eResource().getResourceSet();
		if (resourceSet==null)
			return result;
		for (Resource r : resourceSet.getResources()) {
			if (r instanceof Bpmn2Resource) {
				for (EObject o : r.getContents()) {
					if (o instanceof DocumentRoot) {
						DocumentRoot root = (DocumentRoot)o;
						Definitions defs = root.getDefinitions();
						for (BPMNDiagram d : defs.getDiagrams()) {
							BPMNDiagram bpmnDiagram = (BPMNDiagram)d;
							BaseElement bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
							if (type==0) {
								result.add(bpmnDiagram);
							}
							if ((type & 1)!=0) {
								if (!(bpmnElement instanceof SubProcess)) {
									result.add(bpmnDiagram);
								}
							}
							if ((type & 2)!=0) {
								if (bpmnElement instanceof SubProcess) {
									result.add(bpmnDiagram);
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Check if the given Graphiti Diagram is used to display the contents of a SubProcess.
	 * 
	 * @param diagram the Graphiti Diagram
	 * @return true if the diagram is a SubProcess, false if it is a top-level diagram or
	 *         if indeterminable because the diagram is not yet linked to a BPMN element.
	 */
	public static boolean isSubDiagram(Diagram diagram) {
		BPMNDiagram bpmnDiagram = BusinessObjectUtil.getFirstElementOfType(diagram, BPMNDiagram.class);
		if (bpmnDiagram!=null) {
			BaseElement bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
			return bpmnElement instanceof SubProcess;
		}
		return false;
	}
	
	/**
	 * Find the BPMNDiagram in the editor's Resource Set that references the given BaseElement.
	 * 
	 * If the parameter "contains" is TRUE, then the BaseElement's ancestor hierarchy is searched recursively.
	 * 
	 * The BaseElement may be either a container (i.e. Process, SubProcess, Participant, etc.) or
	 * a simple shape (Task, Gateway, etc.)
	 * 
	 * @param editor
	 * @param baseElement
	 * @param contains
	 * @return
	 */
	public static BPMNDiagram findBPMNDiagram(final BaseElement baseElement, boolean contains) {
		if (baseElement==null || baseElement.eResource()==null)
			return null;
		ResourceSet resourceSet = baseElement.eResource().getResourceSet();
		if (resourceSet==null)
			return null;
		for (Resource r : resourceSet.getResources()) {
			if (r instanceof Bpmn2Resource) {
				for (EObject o : r.getContents()) {
					if (o instanceof DocumentRoot) {
						DocumentRoot root = (DocumentRoot)o;
						Definitions defs = root.getDefinitions();
						BaseElement bpmnElement;
						for (BPMNDiagram d : defs.getDiagrams()) {
							BPMNDiagram bpmnDiagram = (BPMNDiagram)d;
							bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
							if (bpmnElement == baseElement)
								return bpmnDiagram;
						}
						if (contains) {
							for (BPMNDiagram d : defs.getDiagrams()) {
								BPMNDiagram bpmnDiagram = (BPMNDiagram)d;
								for (DiagramElement de : bpmnDiagram.getPlane().getPlaneElement()) {
									if (de instanceof BPMNShape)
										bpmnElement = ((BPMNShape)de).getBpmnElement();
									else if (de instanceof BPMNEdge)
										bpmnElement = ((BPMNEdge)de).getBpmnElement();
									else
										continue;
									if (bpmnElement == baseElement)
										return bpmnDiagram;
								}
							}
							EObject parent = baseElement.eContainer();
							if (parent instanceof BaseElement && !(parent instanceof Definitions)) {
								BPMNDiagram bpmnDiagram = findBPMNDiagram((BaseElement)parent, true);
								if (bpmnDiagram!=null)
									return bpmnDiagram;
							}
						}
//						for (BPMNDiagram d : defs.getDiagrams()) {
//							BPMNDiagram bpmnDiagram = (BPMNDiagram)d;
//							bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
//							if (bpmnElement instanceof Collaboration) {
//								Collaboration collaboration = (Collaboration)bpmnElement;
//								for (Participant p : collaboration.getParticipants()) {
//									if (baseElement==p)
//										return bpmnDiagram;
//									if (baseElement==p.getProcessRef())
//										return bpmnDiagram;
//								}
//							}
//						}
					}
				}
			}
		}
		return null;
	}
	
	public static BPMNDiagram createBPMNDiagram(BaseElement container) {
		Definitions definitions = ModelUtil.getDefinitions(container);
		if (definitions!=null)
			return createBPMNDiagram(definitions, container);
		return null;
	}
	
	public static BPMNDiagram createBPMNDiagram(Definitions definitions, BaseElement container) {
		
		Resource resource = definitions.eResource();
        BPMNDiagram bpmnDiagram = BpmnDiFactory.eINSTANCE.createBPMNDiagram();
		ModelUtil.setID(bpmnDiagram, resource);
        bpmnDiagram.setName(ExtendedPropertiesProvider.getTextValue(container));

		BPMNPlane plane = BpmnDiFactory.eINSTANCE.createBPMNPlane();
		ModelUtil.setID(plane, resource);
		plane.setBpmnElement(container);
		
		bpmnDiagram.setPlane(plane);
		
		// this has to happen last because the IResourceChangeListener in the DesignEditor
		// looks for add/remove to Definitions.diagrams
        definitions.getDiagrams().add(bpmnDiagram);

		return bpmnDiagram;
	}
	
	/**
	 * 
	 * @param baseElement
	 * @return
	 */
	public static BPMNShape findBPMNShape(BaseElement baseElement) {
		Definitions definitions = ModelUtil.getDefinitions(baseElement);
		if (definitions!=null) {
			for (BPMNDiagram d : definitions.getDiagrams()) {
				BPMNShape bpmnShape = findBPMNShape(d, baseElement);
				if (bpmnShape!=null)
					return bpmnShape;
			}
		}
		return null;
	}
	
	public static BPMNShape findBPMNShape(BPMNDiagram bpmnDiagram, BaseElement baseElement) {
		for (DiagramElement de : bpmnDiagram.getPlane().getPlaneElement()) {
			if (de instanceof BPMNShape) {
				if (((BPMNShape)de).getBpmnElement() == baseElement)
					return (BPMNShape)de;
			}
		}
		return null;
	}
	
	public static BPMNEdge findBPMNEdge(EObject baseElement) {
		Definitions definitions = ModelUtil.getDefinitions(baseElement);
		if (definitions!=null) {
			for (BPMNDiagram d : definitions.getDiagrams()) {
				BPMNEdge bpmnEdge = findBPMNEdge(d, baseElement);
				if (bpmnEdge!=null)
					return bpmnEdge;
			}
		}
		return null;
	}
	
	public static BPMNEdge findBPMNEdge(BPMNDiagram bpmnDiagram, EObject baseElement) {
		
		if (bpmnDiagram == null || bpmnDiagram.getPlane() == null)
			return null;
		
		for (DiagramElement de : bpmnDiagram.getPlane().getPlaneElement()) {
			if (de instanceof BPMNEdge) {
				if (((BPMNEdge)de).getBpmnElement() == baseElement)
					return (BPMNEdge)de;
			}
		}
		return null;
	}
	
	public static DiagramElement findDiagramElement(EObject object) {
		Definitions definitions = ModelUtil.getDefinitions(object);
		if (definitions!=null) {
			for (BPMNDiagram d : definitions.getDiagrams()) {
				DiagramElement de = findDiagramElement(d, object);
				if (de!=null)
					return de;
			}
		}
		return null;
	}
	
	public static DiagramElement findDiagramElement(BPMNDiagram bpmnDiagram, EObject object) {
		for (DiagramElement de : bpmnDiagram.getPlane().getPlaneElement()) {
			EStructuralFeature f = de.eClass().getEStructuralFeature("bpmnElement"); //$NON-NLS-1$
			if (f!=null) {
				if (de.eGet(f) == object)
					return de;
			}
		}
		return null;
	}
	
	/**
	 * Returns the BPMNDiagram element that owns the given DiagramElement.
	 * 
	 * @param de
	 * @return
	 */
	public static BPMNDiagram getBPMNDiagram(DiagramElement de) {
		EObject container = de.eContainer();
		while (!(container instanceof Definitions)) {
			if (container instanceof BPMNDiagram)
				return (BPMNDiagram)container;
			container = container.eContainer();
		}
		return null;
	}
	
	public static boolean deleteContainerIfPossible(RootElement rootElement) {
		if (rootElement==null)
			return false;
		
		boolean canDelete = false;
		Definitions definitions = (Definitions)rootElement.eContainer();
		// check if this thing has already been deleted (eContainer == null)
		if (definitions!=null) {
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(rootElement);
			RootElement newRootElement = null;
			if (bpmnDiagram==null) {
				List<EObject> allObjects = new ArrayList<EObject>();
				Map<EObject, Collection<EStructuralFeature.Setting>> usages;
				TreeIterator<EObject> iter = definitions.eAllContents();
				while (iter.hasNext())
					allObjects.add(iter.next());
					
		        usages = UsageCrossReferencer.findAll(allObjects, rootElement);
				canDelete = usages.isEmpty();
			}
			else {
				// find a replacement for the BPMNDiagram target element
				for (RootElement r : definitions.getRootElements()) {
					if (r!=rootElement && (r instanceof FlowElementsContainer || r instanceof Collaboration)) {
						if (rootElement instanceof Collaboration) {
							Collaboration collaboration = (Collaboration) rootElement;
							if (collaboration.getParticipants().isEmpty()) {
								newRootElement = r;
								canDelete = true;
								break;
							}
								
						}
						else if (rootElement instanceof FlowElementsContainer) {
							FlowElementsContainer container = (FlowElementsContainer) rootElement;
							if (container.getFlowElements().isEmpty()) {
								newRootElement = r;
								canDelete = true;
								break;
							}
						}
						break;
					}
				}
			}
			
			if (canDelete) {
				if (bpmnDiagram!=null && newRootElement!=null) {
					EcoreUtil.delete(rootElement);
					bpmnDiagram.getPlane().setBpmnElement(newRootElement);
				}
			}
		}
		return canDelete;
	}
	
	public static void updateDiagramType(Diagram diagram) {
		BPMNDiagram bpmnDiagram = BusinessObjectUtil.getFirstElementOfType(diagram, BPMNDiagram.class);
		if (bpmnDiagram!=null && bpmnDiagram.getPlane()!=null) {
			BaseElement bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
			if (bpmnElement==null) {
				// this BPMNDiagram has had its bpmnElement deleted.
				// make it point to the first valid RootElement.
				Definitions definitions = ModelUtil.getDefinitions(bpmnDiagram);
				for (RootElement r : definitions.getRootElements()) {
					if (r instanceof FlowElementsContainer || r instanceof Collaboration) {
						bpmnDiagram.getPlane().setBpmnElement(r);
						break;
					}
				}
			}
		}
	}

	/**
	 * Returns a list of all PictogramElements that reference the given BaseElement in all Graphiti Diagrams
	 * contained in all Resources of the given ResourceSet
	 * 
	 * @param resourceSet
	 * @param baseElement
	 * @return
	 */
	public static List<PictogramElement> getPictogramElements(ResourceSet resourceSet, BaseElement baseElement) {
		List<PictogramElement> elements = new ArrayList<PictogramElement>();
		for (Resource r : resourceSet.getResources()) {
			for (EObject o : r.getContents()) {
				if (o instanceof Diagram) {
					Diagram diagram = (Diagram)o;
					elements.addAll( Graphiti.getLinkService().getPictogramElements(diagram, baseElement) );
				}
			}
		}
		return elements;
	}
	
	/**
	 * Convenience method to return only the Graphiti ContainerShapes that reference the given BaseElement
	 * in all Diagrams of the given ResourceSet
	 * 
	 * @param resourceSet
	 * @param baseElement
	 * @return
	 */
	public static List<ContainerShape> getContainerShapes(ResourceSet resourceSet, BaseElement baseElement) {
		List<ContainerShape> shapes = new ArrayList<ContainerShape>();
		List<PictogramElement> pes = DIUtils.getPictogramElements(resourceSet, baseElement);
		for (PictogramElement pe : pes) {
			if (pe instanceof ContainerShape) {
				if (BusinessObjectUtil.getFirstElementOfType(pe, BPMNShape.class) != null)
					shapes.add((ContainerShape)pe);
			}
		}
		return shapes;
	}
	
	/**
	 * Convenience method to return only a single Graphiti ContainerShape that
	 * references the given BaseElement in the current Diagram
	 * 
	 * @param baseElement
	 * @return
	 */
	public static ContainerShape getContainerShape(BaseElement baseElement) {
		DiagramEditor diagramEditor = ModelUtil.getDiagramEditor(baseElement);
		if (diagramEditor!=null) {
			Diagram diagram = diagramEditor.getDiagramTypeProvider().getDiagram();
			for (PictogramElement pe : Graphiti.getLinkService().getPictogramElements(diagram, baseElement)) {
				if (pe instanceof ContainerShape && BusinessObjectUtil.getFirstElementOfType(pe, BPMNShape.class)!=null) {
					return (ContainerShape) pe;
				}
			}
		}
		return null;
	}
	
	/**
	 * Convenience method to return only the Graphiti Connections that reference the given BaseElement
	 * in all Diagrams of the given ResourceSet
	 * 
	 * @param resourceSet
	 * @param baseElement
	 * @return
	 */
	public static List<Connection> getConnections(ResourceSet resourceSet, BaseElement baseElement) {
		List<Connection> connections = new ArrayList<Connection>();
		List<PictogramElement> pes = DIUtils.getPictogramElements(resourceSet, baseElement);
		for (PictogramElement pe : pes) {
			if (pe instanceof Connection) {
				if (BusinessObjectUtil.getFirstElementOfType(pe, BPMNEdge.class) != null)
					connections.add((Connection)pe);
			}
		}
		return connections;
	}
	
	/**
	 * Convenience method to return only a single Graphiti Connection that
	 * references the given BaseElement in the current Diagram
	 * 
	 * @param baseElement
	 * @return
	 */
	public static Connection getConnection(BaseElement baseElement) {
		DiagramEditor diagramEditor = ModelUtil.getDiagramEditor(baseElement);
		if (diagramEditor!=null) {
			Diagram diagram = diagramEditor.getDiagramTypeProvider().getDiagram();
			for (Connection c : diagram.getConnections()) {
				if ( BusinessObjectUtil.getFirstBaseElement(c) == baseElement &&
						BusinessObjectUtil.getFirstElementOfType(c, BPMNEdge.class)!=null) {
					return c;
				}
			}
		}
		return null;
	}

	public static Diagram getDiagram(BaseElement baseElement) {
		Resource res = ExtendedPropertiesAdapter.getResource(baseElement);
		if (res == null)
			return null;
		List<PictogramElement> pes = getPictogramElements(res.getResourceSet(), baseElement);
		if (pes.size()>0) {
			return Graphiti.getPeService().getDiagramForPictogramElement(pes.get(0));
		}
		return null;
	}
	
}