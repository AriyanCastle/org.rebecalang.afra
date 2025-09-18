package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.preference.IPreferenceStore;
import org.rebecalang.afra.ideplugin.Activator;
import org.rebecalang.afra.ideplugin.editors.GeneralPreferenceInitializer;

public class RebecaPreferenceInitializer extends GeneralPreferenceInitializer {
	//
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		
		// Existing preferences
		setDefaultAttr(preferences, "Rebeca.SingleLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.MultiLineComment", "00,128,128");
		setDefaultAttr(preferences, "Rebeca.String", "00,00,128");
		setDefaultAttr(preferences, "Rebeca.Default", "00,00,00");
		setDefault(preferences, "Rebeca.KeyWord", "128,00,128", true); // Keep keywords purple and bold
		
		// Enhanced syntax highlighting preferences (light mode friendly)
		setDefaultAttr(preferences, "Rebeca.Type", "00,00,205");           // Dark blue for types
		setDefaultAttr(preferences, "Rebeca.ClassName", "139,69,19");       // Brown for class names
		setDefaultAttr(preferences, "Rebeca.MethodName", "00,100,00");      // Dark green for methods
		setDefaultAttr(preferences, "Rebeca.Number", "255,140,00");         // Orange for numbers
		setDefaultAttr(preferences, "Rebeca.Operator", "105,105,105");      // Dark gray for operators
		setDefaultAttr(preferences, "Rebeca.BuiltinFunction", "30,144,255"); // Medium blue for built-ins
		setDefaultAttr(preferences, "Rebeca.Variable", "184,134,11");       // Dark goldenrod for variables
		
		System.out.println("[Rebeca Preferences] Enhanced color scheme initialized for light mode");
	}
}
