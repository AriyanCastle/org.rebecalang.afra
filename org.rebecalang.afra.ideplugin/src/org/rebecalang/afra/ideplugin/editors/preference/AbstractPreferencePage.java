package org.rebecalang.afra.ideplugin.editors.preference;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public abstract class AbstractPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	@Override
	abstract protected Control createContents(Composite parent);

	public void init(IWorkbench workbench)
	{
	}
}
