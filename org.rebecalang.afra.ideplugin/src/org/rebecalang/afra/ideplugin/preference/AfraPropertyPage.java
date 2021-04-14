package org.rebecalang.afra.ideplugin.preference;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class AfraPropertyPage  extends PropertyPage implements IWorkbenchPropertyPage {
	@Override
	protected Control createContents(Composite parent) {
    
		noDefaultButton();
		
		Composite composite = new Composite(parent, SWT.NULL);
	    GridLayout gridLayout = new GridLayout(2 ,false);
	    composite.setLayout(gridLayout);
		Label labelForType = new Label(composite, SWT.NONE);	
		labelForType.setText("Project Type: ");
		labelForType.setFont(JFaceResources.getFontRegistry().getBold(""));
		
//		Label typeT = new Label(composite, SWT.NONE);
//		typeT.setText("fdg");
//
//		
//	    Label labelForVersion = new Label(composite, SWT.NONE);
//	    labelForVersion.setText("Project Version: ");
//		labelForVersion.setFont(JFaceResources.getFontRegistry().getBold(""));
//
//		Label versionT = new Label(composite, SWT.NONE);
//		versionT.setText("dd");
		
		return composite;
	}
	
}
