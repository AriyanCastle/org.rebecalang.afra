package org.rebecalang.afra.ideplugin.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager
{
	private Map<RGB, Color> colorMap = new HashMap<RGB, Color>();

	public void dispose()
	{
		Iterator<Color> e = colorMap.values().iterator();
		while (e.hasNext())
		{
			((Color)e.next()).dispose();
		}
	}
	public Color getColor(RGB rgb)
	{
		Color color = (Color)colorMap.get(rgb);
		if (color == null)
		{
			color = new Color(Display.getCurrent(), rgb);
			colorMap.put(rgb, color);
		}
		return color;
	}
}
