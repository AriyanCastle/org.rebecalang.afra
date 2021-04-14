package org.rebecalang.afra.ideplugin.editors;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public abstract class GeneralPreferenceInitializer  extends AbstractPreferenceInitializer{

	protected void setDefaultAttr(IPreferenceStore preferences , String name,String color)
	{
		setDefault(preferences, name, color, false);
	}

	protected void setDefault( IPreferenceStore preferences , String name,	String color, boolean bold)
	{
		preferences.setDefault("Color." + name, color);
		preferences.setDefault("Bold." + name, bold);
		preferences.setDefault("Italic." + name, false);
	}
}
