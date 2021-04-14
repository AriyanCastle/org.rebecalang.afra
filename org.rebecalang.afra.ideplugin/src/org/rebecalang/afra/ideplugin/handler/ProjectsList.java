package org.rebecalang.afra.ideplugin.handler;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class ProjectsList extends WorkbenchWindowControlContribution {
	private Combo mReader;
	private static IProject selectedProject;
	
	public static IProject getSelectedProject() {
		return selectedProject;
	}

	public ProjectsList() {

	}

	@Override
	protected Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout glContainer = new GridLayout(1, false);
		glContainer.marginTop = 1;
		glContainer.marginBottom = 0;
		glContainer.marginHeight = 0;
		glContainer.marginWidth = 0;
		container.setLayout(glContainer);
		GridData glReader = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		glReader.widthHint = 180;
		mReader = new Combo(container, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
		mReader.setLayoutData(glReader);
		mReader.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo projects = (Combo)e.getSource();
				String selectedProjectName = projects.getItem(projects.getSelectionIndex());
				
				for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
					if (project.getName().equals(selectedProjectName))
						selectedProject = project;
					break;
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		ResourcesPlugin.getWorkspace().getRoot().getProjects();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject project : projects) {
			if (project.isOpen())
				mReader.add(project.getName());
		}
		if (projects.length > 0)
			mReader.select(0);
		
		return container;
	}

	@Override
	protected int computeWidth(Control control) {
		return 300;
	}
	
	
}