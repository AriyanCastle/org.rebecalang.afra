package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPreferenceInitializer extends GeneralPreferenceInitializer {

	public void initializeDefaultPreferences()
	{
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		/*preferences.setDefault("Rebeca.SingleLineComment", "00,128,128");
		preferences.setDefault("Rebeca.MultiLineComment", "00,128,128");
		preferences.setDefault("Rebeca.String", "00,00,128");
		preferences.setDefault("Rebeca.Default", "00,00,00");
		preferences.setDefault("Rebeca.KeyWord", "128,00,128");*/
/*		Preferences preferences = RebecaUIPlugin.getPlugin()
				.getPluginPreferences();
*/
		setDefaultAttr(preferences, "Rebeca.SingleLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.MultiLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.String", "00,00,128");
		setDefaultAttr(preferences, "Rebeca.Default", "00,00,00");
		setDefault(preferences, "Rebeca.KeyWord", "128,00,128", true);
	}
}
