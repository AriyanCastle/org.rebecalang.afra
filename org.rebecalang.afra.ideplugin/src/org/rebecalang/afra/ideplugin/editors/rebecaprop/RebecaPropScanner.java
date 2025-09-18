package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.rebecalang.afra.ideplugin.editors.ColorManager;

public class RebecaPropScanner extends RuleBasedScanner {
	// Property file keywords and operators
	private static final String[] rebecaPropKeywords = {"property", "define", "Assertion", "LTL", "CTL"};
	
	// Temporal logic operators (common in property specifications)
	private static final String[] temporalOperators = {"G", "F", "X", "U", "R"};
	
	// Boolean literals and common property keywords
	private static final String[] propLiterals = {"true", "false", "Safety"};

	public RebecaPropScanner(ColorManager manager)
	{
		System.out.println("[RebecaProp Scanner] Initializing enhanced property file syntax highlighting...");
		
		// Create tokens for different categories
		IToken keywordToken = new Token(RebecaPropTextAttribute.KEYWORD.getTextAttribute(manager));
		IToken temporalOpToken = new Token(RebecaPropTextAttribute.TEMPORAL_OPERATOR.getTextAttribute(manager));
		IToken literalToken = new Token(RebecaPropTextAttribute.PROPERTY_LITERAL.getTextAttribute(manager));
		IToken defaultToken = new Token(RebecaPropTextAttribute.DEFAULT.getTextAttribute(manager));
	
		List<IRule> rules = new ArrayList<IRule>();
	
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
	
		// Add property file keywords (purple)
		for (String keyword : rebecaPropKeywords) {
			wordRule.addWord(keyword, keywordToken);
			System.out.println("[RebecaProp Scanner] Added keyword: " + keyword);
		}
		
		// Add temporal logic operators (orange)
		for (String operator : temporalOperators) {
			wordRule.addWord(operator, temporalOpToken);
			System.out.println("[RebecaProp Scanner] Added temporal operator: " + operator);
		}
		
		// Add boolean literals and common property types (medium blue)
		for (String literal : propLiterals) {
			wordRule.addWord(literal, literalToken);
			System.out.println("[RebecaProp Scanner] Added property literal: " + literal);
		}
		
		rules.add(wordRule);
	
		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
		
		System.out.println("[RebecaProp Scanner] Enhanced property scanner initialized with " + rules.size() + " rules");
		System.out.println("[RebecaProp Scanner] Keywords: " + rebecaPropKeywords.length + " (purple)");
		System.out.println("[RebecaProp Scanner] Temporal operators: " + temporalOperators.length + " (orange)");
		System.out.println("[RebecaProp Scanner] Property literals: " + propLiterals.length + " (medium blue)");
	}
}
