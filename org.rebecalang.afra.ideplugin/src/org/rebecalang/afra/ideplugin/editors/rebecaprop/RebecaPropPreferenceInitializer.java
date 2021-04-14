package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPropPreferenceInitializer extends GeneralPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences(){
		
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		/*preferences.setDefault("RebecaProp.SingleLineComment", "00,128,128");
		preferences.setDefault("RebecaProp.MultiLineComment", "00,128,128");
		preferences.setDefault("RebecaProp.String", "00,00,128");
		preferences.setDefault("RebecaProp.Default", "00,00,00");
		preferences.setDefault("RebecaProp.Keyword", "128,00,128");*/
		/*Preferences preferences = RebecaUIPlugin.getPlugin()
				.getPluginPreferences();
		*/
		setDefaultAttr(preferences, "RebecaProp.SingleLineComment", "00,128,128");
		setDefaultAttr(preferences, "RebecaProp.MultiLineComment", "00,128,128");
		setDefaultAttr(preferences, "RebecaProp.String", "00,00,128");
		setDefaultAttr(preferences, "RebecaProp.Default", "00,00,00");
		setDefault(preferences, "RebecaProp.KeyWord", "128,00,128", true);
	}
}
