package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;

public final class RebecaTextAttribute extends GeneralTextAttribute {

	public static final RebecaTextAttribute SINGLE_LINE_COMMENT = new RebecaTextAttribute();
	public static final RebecaTextAttribute MULTI_LINE_COMMENT = new RebecaTextAttribute();
	public static final RebecaTextAttribute STRING = new RebecaTextAttribute();
	public static final RebecaTextAttribute DEFAULT = new RebecaTextAttribute();
	public static final RebecaTextAttribute KEY_WORD = new RebecaTextAttribute();

	private RebecaTextAttribute()
	{
		super();
	}
	
	public static void init()
	{
		readColor(SINGLE_LINE_COMMENT, "Rebeca.SingleLineComment");
		readColor(MULTI_LINE_COMMENT, "Rebeca.MultiLineComment");
		readColor(STRING, "Rebeca.String");
		readColor(DEFAULT, "Rebeca.Default");
		readColor(KEY_WORD, "Rebeca.KeyWord");
	}

}
