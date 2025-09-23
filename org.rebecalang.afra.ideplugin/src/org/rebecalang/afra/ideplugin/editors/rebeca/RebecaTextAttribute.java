package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;

public final class RebecaTextAttribute extends GeneralTextAttribute {

	public static final RebecaTextAttribute SINGLE_LINE_COMMENT = new RebecaTextAttribute();
	public static final RebecaTextAttribute MULTI_LINE_COMMENT = new RebecaTextAttribute();
	public static final RebecaTextAttribute STRING = new RebecaTextAttribute();
	public static final RebecaTextAttribute DEFAULT = new RebecaTextAttribute();
	public static final RebecaTextAttribute KEY_WORD = new RebecaTextAttribute();
	
	// Enhanced syntax highlighting attributes
	public static final RebecaTextAttribute TYPE = new RebecaTextAttribute();
	public static final RebecaTextAttribute CLASS_NAME = new RebecaTextAttribute();
	public static final RebecaTextAttribute METHOD_NAME = new RebecaTextAttribute();
	public static final RebecaTextAttribute NUMBER = new RebecaTextAttribute();
	public static final RebecaTextAttribute OPERATOR = new RebecaTextAttribute();
	public static final RebecaTextAttribute BUILTIN_FUNCTION = new RebecaTextAttribute();
	public static final RebecaTextAttribute VARIABLE = new RebecaTextAttribute();
	public static final RebecaTextAttribute PUNCTUATION = new RebecaTextAttribute();

	private RebecaTextAttribute()
	{
		super();
	}
	
	public static void init()
	{
		System.out.println("[Rebeca Syntax] Initializing enhanced syntax highlighting...");
		readColor(SINGLE_LINE_COMMENT, "Rebeca.SingleLineComment");
		readColor(MULTI_LINE_COMMENT, "Rebeca.MultiLineComment");
		readColor(STRING, "Rebeca.String");
		readColor(DEFAULT, "Rebeca.Default");
		readColor(KEY_WORD, "Rebeca.KeyWord");
		
		// Enhanced syntax highlighting
		readColor(TYPE, "Rebeca.Type");
		readColor(CLASS_NAME, "Rebeca.ClassName");
		readColor(METHOD_NAME, "Rebeca.MethodName");
		readColor(NUMBER, "Rebeca.Number");
		readColor(OPERATOR, "Rebeca.Operator");
		readColor(BUILTIN_FUNCTION, "Rebeca.BuiltinFunction");
		readColor(VARIABLE, "Rebeca.Variable");
		readColor(PUNCTUATION, "Rebeca.Punctuation");
		System.out.println("[Rebeca Syntax] Enhanced syntax highlighting initialized successfully!");
	}

}
