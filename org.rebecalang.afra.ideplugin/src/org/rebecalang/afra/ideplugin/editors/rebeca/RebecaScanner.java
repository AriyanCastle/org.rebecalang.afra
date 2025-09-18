package org.rebecalang.afra.ideplugin.editors.rebeca;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.rebecalang.afra.ideplugin.editors.ColorManager;

public class RebecaScanner extends RuleBasedScanner {
	// Language keywords (keep purple) - core language constructs  
	private static final String[] rebecaKeywords = {"reactiveclass", "if", "else",
			"msgsrv", "knownrebecs", "statevars", "main", "self", "sender",
			"externalclass", "sends", "of", "globalvariables"};
	
	// Data types (dark blue)
	private static final String[] rebecaTypes = {"boolean", "byte", "short", "int", "bitint"};
	
	// Built-in functions and literals (medium blue)
	private static final String[] rebecaBuiltins = {"pow", "after", "deadline", "delay", "true", "false"};
	

	public RebecaScanner(ColorManager manager)
	{
		System.out.println("[Rebeca Scanner] Initializing enhanced syntax highlighting...");
		
		// Create tokens for different categories
		IToken keywordToken = new Token(RebecaTextAttribute.KEY_WORD.getTextAttribute(manager));
		IToken typeToken = new Token(RebecaTextAttribute.TYPE.getTextAttribute(manager));
		IToken builtinToken = new Token(RebecaTextAttribute.BUILTIN_FUNCTION.getTextAttribute(manager));
		IToken classNameToken = new Token(RebecaTextAttribute.CLASS_NAME.getTextAttribute(manager));
		IToken methodNameToken = new Token(RebecaTextAttribute.METHOD_NAME.getTextAttribute(manager));
		IToken defaultToken = new Token(RebecaTextAttribute.DEFAULT.getTextAttribute(manager));

		List<IRule> rules = new ArrayList<IRule>();

		// Enhanced word rule for identifiers
		WordRule wordRule = new WordRule(new IWordDetector()
		{
			public boolean isWordPart(char character)
			{
				return Character.isJavaIdentifierPart(character);
			}
			public boolean isWordStart(char character)
			{
				return Character.isJavaIdentifierStart(character);
			}
		}, defaultToken);

		// Add keywords (keep purple and bold)
		for (String keyword : rebecaKeywords) {
			wordRule.addWord(keyword, keywordToken);
			System.out.println("[Rebeca Scanner] Added keyword: " + keyword);
		}
		
		// Add types (dark blue)
		for (String type : rebecaTypes) {
			wordRule.addWord(type, typeToken);
			System.out.println("[Rebeca Scanner] Added type: " + type);
		}
		
		// Add built-in functions and literals (medium blue)
		for (String builtin : rebecaBuiltins) {
			wordRule.addWord(builtin, builtinToken);
		}
		
		// Note: For proper class name and method name detection, we would need
		// more sophisticated pattern matching (similar to Java IDEs)
		// This could be implemented using custom IRule implementations that detect:
		// - Identifiers following "reactiveclass" keyword (class names)
		// - Identifiers following "msgsrv" keyword (method declarations)
		// - Identifiers following "." operator (method calls)
		// For now, the color attributes are ready and can be extended with proper pattern rules
		
		rules.add(wordRule);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
		
		System.out.println("[Rebeca Scanner] Enhanced scanner initialized with " + rules.size() + " rules");
		System.out.println("[Rebeca Scanner] Added class name highlighting (brown)");
		System.out.println("[Rebeca Scanner] Added method name highlighting (dark green)");
		System.out.println("[Rebeca Scanner] Keywords: " + rebecaKeywords.length + " (purple)");
		System.out.println("[Rebeca Scanner] Types: " + rebecaTypes.length + " (dark blue)");
		System.out.println("[Rebeca Scanner] Built-ins: " + rebecaBuiltins.length + " (medium blue)");
	}

}
