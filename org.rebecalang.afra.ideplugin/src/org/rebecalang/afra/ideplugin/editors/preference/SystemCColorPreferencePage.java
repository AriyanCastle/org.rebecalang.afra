package org.rebecalang.afra.ideplugin.editors.preference;

/**
 * Color preference page for SystemC editor
 * 
 * @author amshali
 * 
 */
public class SystemCColorPreferencePage extends AbstractColorPreferencePage {

	public SystemCColorPreferencePage() {
		attributes = new TextAttribute[] { new TextAttribute("SystemC.Default", "Default"),
				new TextAttribute("SystemC.SingleLineComment", "Single line comment"),
				new TextAttribute("SystemC.MultiLineComment", "Multi line comment"),
				new TextAttribute("SystemC.CPP_KeyWord", "C++ reserved word"),
				new TextAttribute("SystemC.Macros", "SystemC macros"),
				new TextAttribute("SystemC.Keyword", "SystemC reserved words"),
				new TextAttribute("SystemC.String", "String") };

	}
}
