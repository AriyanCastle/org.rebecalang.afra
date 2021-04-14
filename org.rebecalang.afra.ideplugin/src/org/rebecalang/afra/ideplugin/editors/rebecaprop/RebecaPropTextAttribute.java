package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;

public class RebecaPropTextAttribute extends GeneralTextAttribute {

	public static final RebecaPropTextAttribute SINGLE_LINE_COMMENT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute MULTI_LINE_COMMENT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute STRING = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute DEFAULT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute KEYWORD = new RebecaPropTextAttribute();

	private RebecaPropTextAttribute()
	{
		super();
	}
		
	public static void init()
	{
		readColor(SINGLE_LINE_COMMENT, "RebecaProp.SingleLineComment");
		readColor(MULTI_LINE_COMMENT, "RebecaProp.MultiLineComment");
		readColor(STRING, "RebecaProp.String");
		readColor(DEFAULT, "RebecaProp.Default");
		readColor(KEYWORD, "RebecaProp.KeyWord");
	}
}
