package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPreferenceInitializer extends GeneralPreferenceInitializer {
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		
		setDefaultAttr(preferences, "Rebeca.SingleLineComment", "63,127,95");
		setDefaultAttr(preferences, "Rebeca.MultiLineComment", "63,127,95");
		setDefaultAttr(preferences, "Rebeca.String", "42,0,255");
		setDefaultAttr(preferences, "Rebeca.Default", "0,0,0");
		setDefault(preferences, "Rebeca.KeyWord", "127,0,85", true);
		setDefaultAttr(preferences, "Rebeca.Type", "0,0,192");
		setDefaultAttr(preferences, "Rebeca.Number", "176,0,64");
		setDefaultAttr(preferences, "Rebeca.Operator", "100,100,100");
		setDefaultAttr(preferences, "Rebeca.BuiltinFunction", "255,140,0");
		setDefaultAttr(preferences, "Rebeca.Punctuation", "128,128,128");
		setDefaultAttr(preferences, "Rebeca.Annotation", "100,100,100");
		
		setDefaultAttr(preferences, "Rebeca.ClassName", "0,0,0");
		setDefaultAttr(preferences, "Rebeca.MethodName", "0,0,0");
		setDefaultAttr(preferences, "Rebeca.Variable", "0,0,0");
	}
}
