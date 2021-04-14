package org.rebecalang.afra.ideplugin.editors.preference;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rebecalang.afra.ideplugin.general.RebecaUIPlugin;

/**
 * This abstract class is needed by plugin.xml class.
 * 
 * @author amshali
 * 
 */
public abstract class AbstractSimplePreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public AbstractSimplePreferencePage() {
		super(GRID);
		setPreferenceStore(RebecaUIPlugin.getPlugin().getPreferenceStore());
	}

	abstract protected void createFieldEditors();

	protected void addStringField(String name, String label) {
		addField(new StringFieldEditor(name, label, getFieldEditorParent()));
	}

	protected void addBooleanField(String name, String label) {
		addField(new BooleanFieldEditor(name, label, getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
}
