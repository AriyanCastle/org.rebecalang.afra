package org.rebecalang.afra.ideplugin.editors;


import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.rebecalang.afra.ideplugin.general.RebecaUIPlugin;

public abstract class GeneralTextAttribute {
	protected RGB color;
	protected int style;
	
	protected GeneralTextAttribute() {
		color = null;
		style = SWT.NORMAL;		
	}
	
	public final TextAttribute getTextAttribute(ColorManager colorManager)
	{
		return new TextAttribute(colorManager.getColor(color), null, style);
	}
	
	protected static void readColor(GeneralTextAttribute target, String key)
	{
		String colorKey = RebecaUIPlugin.getPreferenceString("Color." + key);
		boolean bold = RebecaUIPlugin.getPreferenceBoolean("Bold." + key);
		boolean italic = RebecaUIPlugin.getPreferenceBoolean("Italic." + key);
		try
		{
			
			RGB rgb = StringConverter.asRGB(colorKey);
			target.color = new RGB(rgb.red, rgb.green, rgb.blue);
		
			/*IPreferenceStore temp = Activator.getDefault().getPreferenceStore();
			String colorKey = RebecaUIPlugin.getPreferenceString("Color." + key);
			System.out.println(colorKey);
			target.color = new RGB(255 , 0 , 0);
			target.style = SWT.NORMAL;*/
			
			if (bold)
				target.style |= SWT.BOLD;
			if (italic)
				target.style |= SWT.ITALIC;
		}
		catch (NumberFormatException ex)
		{
		}
	}
}
