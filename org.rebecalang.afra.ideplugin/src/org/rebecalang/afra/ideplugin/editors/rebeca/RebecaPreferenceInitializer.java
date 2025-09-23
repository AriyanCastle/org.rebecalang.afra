package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPreferenceInitializer extends GeneralPreferenceInitializer {
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		
		setDefaultAttr(preferences, "Rebeca.SingleLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.MultiLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.String", "00,00,128");
		setDefaultAttr(preferences, "Rebeca.Default", "00,00,00");
		setDefault(preferences, "Rebeca.KeyWord", "128,00,128", true);
		setDefaultAttr(preferences, "Rebeca.Type", "00,00,205");
		setDefaultAttr(preferences, "Rebeca.ClassName", "139,69,19");
		setDefaultAttr(preferences, "Rebeca.MethodName", "00,100,00");
		setDefaultAttr(preferences, "Rebeca.Number", "255,140,00");
		setDefaultAttr(preferences, "Rebeca.Operator", "105,105,105");
		setDefaultAttr(preferences, "Rebeca.BuiltinFunction", "30,144,255");
		setDefaultAttr(preferences, "Rebeca.Variable", "184,134,11");
		setDefaultAttr(preferences, "Rebeca.Punctuation", "128,128,128");
	}
}
