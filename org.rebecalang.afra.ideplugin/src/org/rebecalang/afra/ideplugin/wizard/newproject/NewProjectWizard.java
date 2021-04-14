package org.rebecalang.afra.ideplugin.wizard.newproject;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewProjectWizard extends Wizard implements INewWizard {

	private WizardNewProjectCreationPage pageOne;
	private NewProjectWizardPageTwo pageTwo;
	 
	public NewProjectWizard() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean performFinish() {
		RebecaProjectSupport.createProject(
				pageOne.getProjectName(), 
				pageOne.getLocationURI(), 
				pageTwo.getType(), 
				pageTwo.getVersion(),
				pageTwo.isRunInSafeMode(),
				pageTwo.isExportStateSpace(),
				pageTwo.isCreateSampleContent());
		return true;
	}

	@Override
    public void addPages() {
        super.addPages();

        pageOne = new WizardNewProjectCreationPage("From Scratch Project Wizard");
        pageOne.setTitle("From Scratch Project");
        pageOne.setDescription("Create a project from scratch.");
        addPage(pageOne);

        pageTwo = new NewProjectWizardPageTwo();
        addPage(pageTwo);
    }
	
	public boolean canFinish() {
		return getContainer().getCurrentPage() == pageTwo;
	}
}
