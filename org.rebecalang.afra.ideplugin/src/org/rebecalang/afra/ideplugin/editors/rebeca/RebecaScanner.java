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
	private static final String[] rebecaWords = {"reactiveclass", "if", "else",
			"msgsrv", "boolean", "byte", "short", "int", "int", "true",
			"false", "knownrebecs", "statevars", "main", "self", "sender",
			"externalclass", "sends", "of", "globalvariables", "bitint"};

	public RebecaScanner(ColorManager manager)
	{
		IToken keyword = new Token(RebecaTextAttribute.KEY_WORD
				.getTextAttribute(manager));
		IToken other = new Token(RebecaTextAttribute.DEFAULT
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

		for (int i = 0; i < rebecaWords.length; i++)
			wordRule.addWord(rebecaWords[i], keyword);
		rules.add(wordRule);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
