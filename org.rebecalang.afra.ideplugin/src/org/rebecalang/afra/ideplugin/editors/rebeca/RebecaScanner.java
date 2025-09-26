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

	public RebecaScanner(ColorManager manager)
	{
		IToken keywordToken = new Token(RebecaTextAttribute.KEY_WORD.getTextAttribute(manager));
		IToken typeToken = new Token(RebecaTextAttribute.TYPE.getTextAttribute(manager));
		IToken builtinToken = new Token(RebecaTextAttribute.BUILTIN_FUNCTION.getTextAttribute(manager));
		IToken numberToken = new Token(RebecaTextAttribute.NUMBER.getTextAttribute(manager));
		IToken operatorToken = new Token(RebecaTextAttribute.OPERATOR.getTextAttribute(manager));
		IToken punctuationToken = new Token(RebecaTextAttribute.PUNCTUATION.getTextAttribute(manager));
		IToken annotationToken = new Token(RebecaTextAttribute.ANNOTATION.getTextAttribute(manager));
		IToken defaultToken = new Token(RebecaTextAttribute.DEFAULT.getTextAttribute(manager));

		List<IRule> rules = new ArrayList<IRule>();

		rules.add(new RebecaAnnotationRule(annotationToken));
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

		for (String keyword : RebecaConstants.KEYWORDS) {
			wordRule.addWord(keyword, keywordToken);
		}
		
		for (String type : RebecaConstants.TYPES) {
			wordRule.addWord(type, typeToken);
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