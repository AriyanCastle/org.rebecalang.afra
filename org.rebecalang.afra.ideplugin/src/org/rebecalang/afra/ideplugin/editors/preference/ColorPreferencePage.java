package org.rebecalang.afra.ideplugin.editors.preference;

/**
 * Color Preference page for Rebeca editor
 * 
 * @author amshali
 */
public class ColorPreferencePage extends AbstractColorPreferencePage {

	public ColorPreferencePage() {
		attributes = new TextAttribute[] { new TextAttribute("Rebeca.Default", "Default"),
				new TextAttribute("Rebeca.SingleLineComment", "Single line comment"),
				new TextAttribute("Rebeca.MultiLineComment", "Multi line comment"),
				new TextAttribute("Rebeca.KeyWord", "Reserved word"),
				new TextAttribute("Rebeca.String", "String") };
	}

}
