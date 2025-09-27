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
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaConstants;
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaNumberRule;
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaOperatorRule;
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaPunctuationRule;

public class RebecaPropScanner extends RuleBasedScanner {

	public RebecaPropScanner(ColorManager manager)
	{
		IToken keywordToken = new Token(RebecaPropTextAttribute.KEYWORD.getTextAttribute(manager));
		IToken temporalOpToken = new Token(RebecaPropTextAttribute.TEMPORAL_OPERATOR.getTextAttribute(manager));
		IToken literalToken = new Token(RebecaPropTextAttribute.PROPERTY_LITERAL.getTextAttribute(manager));
		IToken builtinToken = new Token(RebecaPropTextAttribute.BUILTIN_FUNCTION.getTextAttribute(manager));
		IToken numberToken = new Token(RebecaPropTextAttribute.NUMBER.getTextAttribute(manager));
		IToken operatorToken = new Token(RebecaPropTextAttribute.OPERATOR.getTextAttribute(manager));
		IToken punctuationToken = new Token(RebecaPropTextAttribute.PUNCTUATION.getTextAttribute(manager));
		IToken defaultToken = new Token(RebecaPropTextAttribute.DEFAULT.getTextAttribute(manager));
	
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new RebecaNumberRule(numberToken));
		rules.add(new RebecaOperatorRule(operatorToken));
		rules.add(new RebecaPunctuationRule(punctuationToken));
	
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
	
		for (String keyword : RebecaConstants.PROPERTY_KEYWORDS) {
			wordRule.addWord(keyword, keywordToken);
		}
		
		for (String operator : RebecaConstants.TEMPORAL_OPERATORS) {
			wordRule.addWord(operator, temporalOpToken);
		}
		
		for (String literal : RebecaConstants.PROPERTY_LITERALS) {
			wordRule.addWord(literal, literalToken);
		}
		
		for (String builtin : RebecaConstants.BUILTINS) {
			wordRule.addWord(builtin, builtinToken);
		}
		
		rules.add(wordRule);
	
		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
