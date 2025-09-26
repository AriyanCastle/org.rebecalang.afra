package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;

public class RebecaPropTextAttribute extends GeneralTextAttribute {

	public static final RebecaPropTextAttribute SINGLE_LINE_COMMENT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute MULTI_LINE_COMMENT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute STRING = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute DEFAULT = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute KEYWORD = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute TEMPORAL_OPERATOR = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute PROPERTY_LITERAL = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute NUMBER = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute OPERATOR = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute PUNCTUATION = new RebecaPropTextAttribute();
	public static final RebecaPropTextAttribute BUILTIN_FUNCTION = new RebecaPropTextAttribute();

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
		readColor(TEMPORAL_OPERATOR, "RebecaProp.TemporalOperator");
		readColor(PROPERTY_LITERAL, "RebecaProp.PropertyLiteral");
		readColor(NUMBER, "RebecaProp.Number");
		readColor(OPERATOR, "RebecaProp.Operator");
		readColor(PUNCTUATION, "RebecaProp.Punctuation");
		readColor(BUILTIN_FUNCTION, "RebecaProp.BuiltinFunction");
	}
}
