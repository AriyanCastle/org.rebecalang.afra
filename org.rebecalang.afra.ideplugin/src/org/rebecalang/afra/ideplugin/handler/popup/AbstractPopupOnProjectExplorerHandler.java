package org.rebecalang.afra.ideplugin.handler.popup;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;

public class AbstractPopupOnProjectExplorerHandler {

	protected static Object getSelectedItem() {
		ISelectionService service = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
	            .getSelectionService();
		IStructuredSelection structured = (IStructuredSelection) service
	            .getSelection("org.eclipse.ui.navigator.ProjectExplorer");
		return structured.getFirstElement();
	}
	
	protected static boolean isSelectedItemIsFileAndHasExtension(String extension) {
		Object selectedItem = getSelectedItem();
		if (!(selectedItem instanceof IFile))
			return false;

		IFile file = (IFile) selectedItem;
		return file.getFileExtension().equals(extension);		
	}

}
