package org.eclipse.bpmn2.modeler.ui.property.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;

public class ShowHideElementsDialog extends ListSelectionDialog {
	
	public final static int MESSAGE_LABELS = 0;
	public final static int MESSAGE_ICONS = 1;
	public final static int MESSAGE_FLOWS = 2;
	public final static int GATEWAY_LABELS = 3;
	public final static int EVENT_LABELS = 4;
	public final static int SEQUENCEFLOW_LABELS = 5;
	
	static private List<String> elements = new ArrayList<String>();
	static {
		elements.add(Messages.ShowHideElementsDialog_Message_Labels);
		elements.add(Messages.ShowHideElementsDialog_Message_Icons);
		elements.add(Messages.ShowHideElementsDialog_Message_Flows);
		elements.add(Messages.ShowHideElementsDialog_Gateway_Labels);
		elements.add(Messages.ShowHideElementsDialog_Event_Labels);
		elements.add(Messages.ShowHideElementsDialog_SequenceFlow_Labels);
	}
	static private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return elements.toArray();
		}
		
	};
	static private LabelProvider labelProvider = new LabelProvider();

	public List<Integer> getSelections() {
		Object result[] = super.getResult();
		List<Integer> indexes = new ArrayList<Integer>();
		int index = 0;
		for (String e : elements) {
			for (Object r : result) {
				if (e.equals(r))
					indexes.add(new Integer(index));
			}
			++index;
		}
		return indexes;
	}

	public ShowHideElementsDialog(Shell shell) {
		super(shell, elements, contentProvider, labelProvider,
				Messages.ShowHideElementsDialog_Prompt);
		setTitle(Messages.ShowHideElementsDialog_Title);
	}
}
