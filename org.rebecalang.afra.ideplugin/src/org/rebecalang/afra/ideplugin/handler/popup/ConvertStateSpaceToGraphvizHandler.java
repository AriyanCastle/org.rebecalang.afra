package org.rebecalang.afra.ideplugin.handler.popup;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.widgets.Shell;
import org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.statespacetransformer.StateSpaceTransformer;
import org.rebecalang.statespacetransformer.StateSpaceTransformingFeature;

public class ConvertStateSpaceToGraphvizHandler extends AbstractPopupOnProjectExplorerHandler {

	public final static String ID = ConvertStateSpaceToGraphvizHandler.class.getName();

	@CanExecute
	public boolean canExecute(MPart part) {
		return isSelectedItemIsFileAndHasExtension("statespace");
	}

	@Execute
	public void execute(Shell shell) {
		IFile selectedFile = (IFile)getSelectedItem();
		IProject project = selectedFile.getProject();
		File graphvizFile = CompilationAndCodeGenerationProcess.getFileFromByReplacingExtension(selectedFile, "dot");
		String[] args = {
				"--extension",
				CoreRebecaProjectPropertyPage.getProjectType(project),
				"--source",
				selectedFile.getLocation().toString(),
				"--output",
				graphvizFile.getAbsolutePath(),
				"--targetmodel",
				StateSpaceTransformingFeature.GRAPH_VIZ.name()
		};
		StateSpaceTransformer.main(args);
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
