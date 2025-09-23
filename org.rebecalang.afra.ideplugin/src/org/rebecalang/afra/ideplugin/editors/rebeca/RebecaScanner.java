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
		System.out.println("[Rebeca Scanner] Initializing Phase 1 enhanced syntax highlighting...");
		
		// Create tokens for different categories
		IToken keywordToken = new Token(RebecaTextAttribute.KEY_WORD.getTextAttribute(manager));
		IToken typeToken = new Token(RebecaTextAttribute.TYPE.getTextAttribute(manager));
		IToken builtinToken = new Token(RebecaTextAttribute.BUILTIN_FUNCTION.getTextAttribute(manager));
		IToken classNameToken = new Token(RebecaTextAttribute.CLASS_NAME.getTextAttribute(manager));
		IToken methodNameToken = new Token(RebecaTextAttribute.METHOD_NAME.getTextAttribute(manager));
		IToken numberToken = new Token(RebecaTextAttribute.NUMBER.getTextAttribute(manager));
		IToken operatorToken = new Token(RebecaTextAttribute.OPERATOR.getTextAttribute(manager));
		IToken punctuationToken = new Token(RebecaTextAttribute.PUNCTUATION.getTextAttribute(manager));
		IToken defaultToken = new Token(RebecaTextAttribute.DEFAULT.getTextAttribute(manager));

		List<IRule> rules = new ArrayList<IRule>();

		// Phase 1: Add new advanced highlighting rules
		// Numbers: integers, decimals, hex, binary (orange)
		rules.add(new RebecaNumberRule(numberToken));
		System.out.println("[Rebeca Scanner] Added number highlighting (orange)");
		
		// Operators: arithmetic, logical, comparison (dark gray)  
		rules.add(new RebecaOperatorRule(operatorToken));
		System.out.println("[Rebeca Scanner] Added operator highlighting (dark gray)");
		
		// Punctuation: braces, parentheses, brackets, semicolons, commas (medium gray)
		rules.add(new RebecaPunctuationRule(punctuationToken));
		System.out.println("[Rebeca Scanner] Added punctuation highlighting (medium gray)");

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
		}
		
		// Add types (dark blue)
		for (String type : rebecaTypes) {
			wordRule.addWord(type, typeToken);
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
		
		System.out.println("[Rebeca Scanner] Phase 1 enhanced scanner initialized with " + rules.size() + " rules");
		System.out.println("[Rebeca Scanner] ✅ Numbers: integers, decimals, hex, binary (orange)");
		System.out.println("[Rebeca Scanner] ✅ Operators: +,-,*,/,%,==,!=,<,>,<=,>=,&&,||,! (dark gray)");
		System.out.println("[Rebeca Scanner] ✅ Punctuation: {}()[];,. (medium gray)");
		System.out.println("[Rebeca Scanner] ✅ Keywords: " + rebecaKeywords.length + " (purple, bold)");
		System.out.println("[Rebeca Scanner] ✅ Types: " + rebecaTypes.length + " (dark blue)");
		System.out.println("[Rebeca Scanner] ✅ Built-ins: " + rebecaBuiltins.length + " (medium blue)");
		System.out.println("[Rebeca Scanner] 🔄 Class/Method names ready for Phase 2 contextual detection");
	}

}
