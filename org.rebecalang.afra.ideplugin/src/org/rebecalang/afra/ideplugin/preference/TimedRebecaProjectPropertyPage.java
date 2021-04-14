package org.rebecalang.afra.ideplugin.preference;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;


public class TimedRebecaProjectPropertyPage extends AbstractRebecaProjectPropertyPage {

	private Button TTS;
	private Button FTTS;

	public static void setProjectSemanticsModelIsTTS(IProject project, boolean value) {
		setProjectAttribute(project, "timedrebeca-semanticsmodel", Boolean.toString(value));
	}
	public static boolean getProjectSemanticsModelIsTTS(IProject project) {
		return Boolean.parseBoolean(getProjectAttribute(project, "timedrebeca-semanticsmodel", "true"));
	}
	

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		Composite container = (Composite) super.createContents(parent);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;

		// *********** Compile Algorithm Section ***********//
		Group semanticsModel = new Group(container, SWT.SHADOW_IN);
		semanticsModel.setText("Semantics Model of Model Checking");
		semanticsModel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		semanticsModel.setLayout(new GridLayout());

		TTS = new Button(semanticsModel, SWT.RADIO);
		TTS.setText("TTS");
		TTS.setSelection(getProjectSemanticsModelIsTTS(getProject()));
		TTS.pack();

		FTTS = new Button(semanticsModel, SWT.RADIO);
		FTTS.setText("FTTS");
		FTTS.setSelection(!getProjectSemanticsModelIsTTS(getProject()));
		FTTS.pack();

		return container;
	}


	protected void performDefaults() {
		super.performDefaults();
		TTS.setSelection(true);
		FTTS.setSelection(false);
	}

	public boolean performOk() {
		if (!super.performOk())
			return false;
		setProjectSemanticsModelIsTTS(getProject(), TTS.getSelection());
		return true;
	}

}
