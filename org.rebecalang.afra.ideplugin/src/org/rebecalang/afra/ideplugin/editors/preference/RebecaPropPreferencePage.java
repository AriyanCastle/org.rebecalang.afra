package org.rebecalang.afra.ideplugin.editors.preference;

/**
 * Color Preference page for Rebeca property editor
 * 
 * @author amshali
 * 
 */
public class RebecaPropPreferencePage extends AbstractColorPreferencePage {

	public RebecaPropPreferencePage() {
		attributes = new TextAttribute[] {
				new TextAttribute("RebecaProp.Default", "Default"),
				new TextAttribute("RebecaProp.SingleLineComment", "Single line comment"),
				new TextAttribute("RebecaProp.MultiLineComment", "Multi line comment"),
				new TextAttribute("RebecaProp.KeyWord", "Reserved words"),
				new TextAttribute("RebecaProp.String", "String") };
	}

}
