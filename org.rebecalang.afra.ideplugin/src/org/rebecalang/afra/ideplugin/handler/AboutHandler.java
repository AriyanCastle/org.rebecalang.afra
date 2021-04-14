package org.rebecalang.afra.ideplugin.handler;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;


public class AboutHandler {

	@CanExecute
	public boolean canExecute(EPartService partService) {
		System.out.println(98);
		return partService != null;
	}

	@Execute
	public void execute(Shell shell) {
		MessageDialog.openInformation(shell, "About", "Afra 3.0");
	}
//	@Execute
	public void execute(IWorkbench workbench) {
		//	MessageDialog.openInformation(shell, "About", "Afra 3.0");
		System.out.println(13);
	}
}
