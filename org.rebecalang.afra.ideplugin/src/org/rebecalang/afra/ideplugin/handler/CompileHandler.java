package org.rebecalang.afra.ideplugin.handler;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;

public class CompileHandler extends AbstractAnalysisHandler {

	@CanExecute
	public boolean canExecute(EPartService partService) {
		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		return validateActiveFile(codeEditor);
	}

	@Execute
	public void execute(Shell shell) {
		CompilationAndCodeGenerationProcess compilationAndCodeGenerationProcess = new 
				CompilationAndCodeGenerationProcess();
		try {
			CompilationStatus compilationStatus = compilationAndCodeGenerationProcess.compileAndGenerateCodes(shell, true);
			switch (compilationStatus) {
			case CANCELED:
				return;
			case SUCCESSFUL:
				MessageDialog.openInformation(shell, "Compilation Report",
						"The model is compiled successfully.");
				break;
			case RESOURCE_DOES_NOE_EXIST:
				MessageDialog.openInformation(shell, "Compilation Error",
						"The corresponding Rebeca file does not exist.");
				break;
			case FAILED:
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.showView("org.eclipse.ui.views.ProblemView");
				break;
			}
		} catch (CoreException | InvocationTargetException | InterruptedException e) {
			MessageDialog.openError(shell, "Internal Error", e.getMessage());
			e.printStackTrace();
		}
	}
}
