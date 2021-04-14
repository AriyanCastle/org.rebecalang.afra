/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.rebecalang.afra.ideplugin.view;

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
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.analysisresult.CheckedProperty;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.analysisresult.ModelCheckingReport;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.analysisresult.SystemInfo;

/**
 * The Problems view is the view supplied by the IDE to show problems.
 * 
 * @since 3.4
 */
public class AnalysisResultView extends ViewPart {

	private TreeViewer analysisResultViewerTree;
	private Composite parent;

	class ViewContentProvider implements ITreeContentProvider {

		public ViewContentProvider() {

		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (report == null)
				return;

			parent.layout(true);
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (report == null)
				return new Object[0];
			return new Object[] { report.getSystemInfo(), report.getCheckedProperty() };
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			Object[][] childs = null;
			if (parentElement instanceof SystemInfo) {
				SystemInfo systemInfo = (SystemInfo) parentElement;
				childs = new Object[4][];
				childs[0] = new String[]{"Total Spent Time", systemInfo.getTotalSpentTime().toString()};
				childs[1] = new String[]{"Number of Reached States", systemInfo.getReachedStates().toString()};
				childs[2] = new String[]{"Number of Reached Transitions", systemInfo.getReachedTransitions().toString()};
				childs[3] = new String[]{"Consumed Memory", systemInfo.getConsumedMem().toString()};
			} else if (parentElement instanceof CheckedProperty) {
				CheckedProperty checkedProperty = (CheckedProperty) parentElement;
				if (checkedProperty.getMessage() == null) {
					childs = new Object[3][];
				} else {
					childs = new Object[4][];
					childs[3] = new String[]{"Message", checkedProperty.getMessage()};
				}
				childs[0] = new String[]{"Property Name", checkedProperty.getName()};
				childs[1] = new String[]{"Property Type", checkedProperty.getType()};
				childs[2] = new String[]{"Analysis Result", checkedProperty.getResult()};
			}
			return childs;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element instanceof ModelCheckingReport) || (element instanceof SystemInfo)
					|| (element instanceof CheckedProperty);
		}
	}

	public void update() {
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
		viewer.expandAll();
		parent.layout(true);
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		this.parent = parent;
		update();
	}

	private ModelCheckingReport report;

	@Override
	public void setFocus() {
		analysisResultViewerTree.getControl().setFocus();
	}

	public void setReport(ModelCheckingReport modelCheckingReport) {
		this.report = modelCheckingReport;
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
			return element.getClass().getSimpleName();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return (element instanceof String[]) ? ((String[]) element)[columnIndex] : element.getClass().getSimpleName();
		}
	}
}
