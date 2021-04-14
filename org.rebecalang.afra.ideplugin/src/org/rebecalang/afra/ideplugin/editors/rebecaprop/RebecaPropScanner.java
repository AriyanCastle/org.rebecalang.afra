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
	private static final String[] rebecaPropWords = {"define", "CTL", "LTL", "property"};

	public RebecaPropScanner(ColorManager manager)
	{
		IToken keyword = new Token(RebecaPropTextAttribute.KEYWORD
				.getTextAttribute(manager));
		IToken other = new Token(RebecaPropTextAttribute.DEFAULT
				.getTextAttribute(manager));
	
		List<WordRule> rules = new ArrayList<WordRule>();
	
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
		}, other);
	
		for (int i = 0; i < rebecaPropWords.length; i++)
			wordRule.addWord(rebecaPropWords[i], keyword);
		rules.add(wordRule);
	
		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
