package org.eclipse.bpmn2.modeler.core.merrimac;

import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;

/**
 * This class handles redrawing of the entire Property Page Form contents. It
 * uses a UIJob to do this: multiple calls in quick succession (less than
 * UIJOB_INTERRUPT_INTERVAL milliseconds) will cause the job to be restarted.
 * This prevents the Form being redrawn multiple times even if no visible
 * changes have occurred since the last redraw.
 * 
 * NOTE: All resize event listeners that call this class' redraw() method
 * MUST first call needsRedraw(). If this method returns false, the resize
 * listener MUST NOT call redraw().
 */
public class Bpmn2PropertyPageRedrawHandler {

	private static int UIJOB_INTERRUPT_INTERVAL = 150;
	private static int NEEDS_REDRAW_INTERVAL = 1000;
	
	static Hashtable<Composite, Bpmn2PropertyPageRedrawHandler> handlers = new Hashtable<Composite, Bpmn2PropertyPageRedrawHandler>();

	UIJob job;
	long lastRedrawTime;
	final Composite rootComposite;
	
	/**
	 * Constructor for Redraw Handler. This is only created in response to
	 * a redraw() request.
	 * 
	 * @param rootComposite
	 */
	private Bpmn2PropertyPageRedrawHandler(final Composite rootComposite) {
		this.rootComposite = rootComposite;
		rootComposite.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (e.widget!=rootComposite)
					Bpmn2PropertyPageRedrawHandler.dispose(rootComposite);
				else if (e.widget instanceof Composite)
					Bpmn2PropertyPageRedrawHandler.dispose((Composite)e.widget);
			}
		});
	}

	/**
	 * Redraw the root composite that contains the given child.
	 * 
	 * @param child
	 */
	public static void redraw(Composite child) {
		Bpmn2PropertyPageRedrawHandler handler = findHandler(child);
		if (handler!=null)
			handler.scheduleRedrawPage();
	}

	/**
	 * Check if the root composite that contains the given child needs
	 * to be redrawn. This is calculated from the time of the last redraw
	 * and prevents an infinite loop of redraws caused by resize listeners.
	 * 
	 * @param child
	 * @return
	 */
	public static boolean needsRedraw(Composite child) {
		Bpmn2PropertyPageRedrawHandler handler = findHandler(child);
		if (handler!=null) {
			return System.currentTimeMillis() - handler.lastRedrawTime > NEEDS_REDRAW_INTERVAL;
		}
		return false;
	}

	/**
	 * Remove the redraw handler for the given root composite.
	 * This prevents the static handler list from growing out of control.
	 * 
	 * @param child
	 */
	public static void dispose(Composite child) {
		Bpmn2PropertyPageRedrawHandler handler = findHandler(child);
		if (handler!=null) {
			if (handler.job!=null)
				handler.job.cancel();
			handlers.remove(handler.rootComposite);
		}
	}
	
	/**
	 * Schedule a redraw. If the UIJob is already running but in a WAIT
	 * state, cancel and reschedule it.
	 */
	private synchronized void scheduleRedrawPage() {
		if (job==null) {
			job = new UIJob("BPMN2 Property Page redraw") { //$NON-NLS-1$

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						doRedrawPage();
					}
					catch (Exception e) {
						// this can happen if the editor is already
						// closed by the time the UI thread runs - ignore
					}
					return Status.OK_STATUS;
				}
				
			};
		}
		if (job.getState() == Job.WAITING)
			job.cancel();
		job.schedule(UIJOB_INTERRUPT_INTERVAL);
	}
	
	/**
	 * Force a re-layout and redraw of the root composite by resizing.
	 * This is the only way I've found of really forcing the Form widgets
	 * to re-layout and redraw themselves.
	 */
	private void doRedrawPage() {
		if (!rootComposite.isDisposed()) {
			rootComposite.setRedraw(false);
			rootComposite.layout();
			Point p = rootComposite.getSize();
			p.x++;
			p.y++;
			rootComposite.setSize(p);
			p.x--;
			p.y--;
			rootComposite.setSize(p);
			rootComposite.setRedraw(true);
			lastRedrawTime = System.currentTimeMillis();
		}		
	}

	/**
	 * Find the redraw handler for the child Composite.
	 * 
	 * @param child
	 * @return
	 */
	private static Bpmn2PropertyPageRedrawHandler findHandler(Composite child) {
		Bpmn2PropertyPageRedrawHandler handler = null;
		Composite composite = findRootComposite(child);
		if (composite!=null) {
			if (handlers.containsKey(composite))
				handler = handlers.get(composite);
			else if (!composite.isDisposed()) {
				handler = new Bpmn2PropertyPageRedrawHandler(composite);
				handlers.put(composite, handler);
			}
		}
		return handler;
	}
	
	/**
	 * Finds the top-level Composite that contains the child widget.
	 * This is the parent of the ScrolledComposite which hosts the
	 * property page Form.
	 * 
	 * @param child
	 * @return
	 */
	private static Composite findRootComposite(Composite child) {
		if (child!=null) {
			if (handlers.containsKey(child))
				return child;
			Composite composite = child.getParent();
			while (composite!=null && !(composite instanceof ScrolledComposite)) {
				composite = composite.getParent();
			}
			if (composite!=null)
				composite = composite.getParent();
			return composite;
		}
		return null;
	}
}
