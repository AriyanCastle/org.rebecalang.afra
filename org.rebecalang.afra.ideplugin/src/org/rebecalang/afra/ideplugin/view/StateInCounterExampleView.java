package org.rebecalang.afra.ideplugin.view;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

//import javax.annotation.PostConstruct;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.Message;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.Queue;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.Rebec;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.State;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.Statevariables;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.Variable;

public class StateInCounterExampleView extends ViewPart {

	private TreeViewer stateContentTree;
	private Composite parent;
	private State state;

	class ViewContentProvider implements ITreeContentProvider {

		public ViewContentProvider() {

		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (state == null)
				return;

			parent.layout(true);
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (state == null)
				return new Object[0];
			return state.getRebec().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			Object[] childs = null;
			ArrayList<Object> items = new ArrayList<>();
			if (parentElement instanceof Rebec) {
				Rebec parent = (Rebec) parentElement;
				items.add(new Object[] { "State Variables", parent.getStatevariables() });
				items.add(new Object[] { "Queue Content", parent.getQueue()});
				if (parent.getNow() != null)
					items.add(new String[] { "Now", (parent.getNow().intValue() == Integer.MAX_VALUE) ? "infinity" : parent.getNow().toString() });
				if (parent.getPc() != null)
					items.add(new String[] { "Program Counter", parent.getPc().toString() });
				if (parent.getRes() != null)
					items.add(new String[] { "Resuming Time", parent.getRes().toString() });
			} else if (parentElement instanceof Object[]) {
				Object secondElement = ((Object[]) parentElement)[1];
				if (secondElement instanceof Statevariables) {
					Statevariables variables = (Statevariables) secondElement;
					for (Variable variable : variables.getVariable()) {
						items.add(new String[] { variable.getName(), variable.getValue() });
					}
				} else if (secondElement instanceof Queue) {
					Queue queue = (Queue) secondElement;
					for (Message message : queue.getMessage()) {
						String messageText = message.getValue();
						if (message.getArrival() != null)
							messageText += " arrival(" + message.getArrival() + ")";
						if (message.getDeadline() != null)
							messageText += " deadline(" + message.getDeadline() + ")";
						items.add(new String[] { messageText, "from " + message.getSender()});
					}
				}
			}
			childs = new Object[items.size()];
			for (int cnt = 0; cnt < items.size(); cnt++)
				childs[cnt] = items.get(cnt);
			return childs;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof Rebec)
				return true;
			if (element instanceof Object[]) {
				Object[] elements = (Object[]) element;
				if (elements[1] instanceof Statevariables)
					return !((Statevariables)elements[1]).getVariable().isEmpty();
				if (elements[1] instanceof Queue)
					return !((Queue)elements[1]).getMessage().isEmpty();
			}
			return false;
		}
	}

	public void update(State state) {
		this.state = state;
		for (Control control : parent.getChildren()) {
			control.dispose();
		}

		final TreeViewer viewer = new TreeViewer(parent);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ViewContentProvider());
		viewer.getTree().setHeaderVisible(true);
		viewer.getTree().setLinesVisible(true);
		viewer.setLabelProvider(new AnalysisResultLabelProvider());
		viewer.setInput(""); // pass a non-null that will be ignored

		TreeColumn mainColumn = new TreeColumn(viewer.getTree(), SWT.LEFT);
		mainColumn.setText("Attribute");
		mainColumn.setWidth(200);
		mainColumn.setAlignment(SWT.CENTER);

		TreeColumn valueColumn = new TreeColumn(viewer.getTree(), SWT.CENTER);
		valueColumn.setText("Value");
		valueColumn.setWidth(200);
		valueColumn.setAlignment(SWT.LEFT);
		viewer.expandToLevel(2);
		parent.layout(true);
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		this.parent = parent;
		update(null);
	}

	@Override
	public void setFocus() {
		stateContentTree.getControl().setFocus();
	}

	class AnalysisResultLabelProvider implements ILabelProvider, ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			return (element instanceof Rebec) ? ((Rebec) element).getName()
					: (element instanceof Statevariables) ? "State Variables"
							: (element instanceof Queue) ? "Queue Content"
									: (element instanceof Variable) ? ((Variable) element).getName()
											: (element instanceof String[]) ? ((String[]) element)[0]
													: element.getClass().getSimpleName();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return (element instanceof String[]) ? ((String[]) element)[columnIndex]
					: (element instanceof Object[]) ? (columnIndex == 0 ? (String) ((Object[]) element)[0] : "") : "";
		}
	}
}
