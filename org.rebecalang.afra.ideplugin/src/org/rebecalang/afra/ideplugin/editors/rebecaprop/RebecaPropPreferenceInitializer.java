package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPropPreferenceInitializer extends GeneralPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		
		setDefaultAttr(preferences, "RebecaProp.SingleLineComment", "63,127,95");
		setDefaultAttr(preferences, "RebecaProp.MultiLineComment", "63,127,95");
		setDefaultAttr(preferences, "RebecaProp.String", "42,0,255");
		setDefaultAttr(preferences, "RebecaProp.Default", "0,0,0");
		setDefault(preferences, "RebecaProp.KeyWord", "127,0,85", true);
		setDefaultAttr(preferences, "RebecaProp.TemporalOperator", "255,140,0");
		setDefaultAttr(preferences, "RebecaProp.PropertyLiteral", "139,0,139");
		setDefaultAttr(preferences, "RebecaProp.Number", "176,0,64");
		setDefaultAttr(preferences, "RebecaProp.Operator", "100,100,100");
		setDefaultAttr(preferences, "RebecaProp.Punctuation", "128,128,128");
		setDefaultAttr(preferences, "RebecaProp.BuiltinFunction", "255,140,0");
	}
}
