package org.rebecalang.afra.ideplugin.view;

import java.util.Collection;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.E4PartWrapper;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IServiceLocator;

@SuppressWarnings("restriction")
public class ViewUtils {
	public static ViewPart getViewPart(String id) {
		try {
			E4PartWrapper newPart = (E4PartWrapper) PlatformUI.getWorkbench().getActiveWorkbenchWindow().
					getActivePage().showView(id);
			EPartService partService = 
					(EPartService) newPart.getViewSite().getService(EPartService.class); 
			return (ViewPart) partService.findPart(id).getObject();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void counterExampleVisible(boolean visibility) {
		IServiceLocator serviceLocator = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		EPartService service = serviceLocator.getService(EPartService.class);
		Collection<MPart> parts = service.getParts();//PartDescriptor("org.rebecalang.afra.ideplugin.partstack.counterexample");
		MPart compositPart = null;
		for (MPart part : parts) {
			if (part.getElementId().equals(CounterExampleGraphView.COMPOSIT_ID))
				compositPart = part;
		}
		if (visibility)
			service.showPart(compositPart, PartState.ACTIVATE);
		else
			service.hidePart(compositPart, false);
		
	}
	
}
