package org.rebecalang.afra.ideplugin.editors.rebeca;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.rebecalang.afra.ideplugin.editors.GeneralRuleBasedPartitionScanner;

/**
 * parse comment
 */
public class RebecaPartitionScanner extends GeneralRuleBasedPartitionScanner {
	
	public static final String SINGLE_LINE_COMMENT = "__rebeca_singleline_comment";

	public static final String MULTI_LINE_COMMENT = "__rebeca_multiline_comment";

	public static final String STRING = "__rebeca_string";

	public RebecaPartitionScanner()
	{
		super();
		IToken string = new Token(STRING);
		IToken multiLineComment = new Token(MULTI_LINE_COMMENT);
		IToken singleLineComment = new Token(SINGLE_LINE_COMMENT);

		List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

		// single line comments.
		rules.add(new EndOfLineRule("//", singleLineComment));

		// strings.
		rules.add(new SingleLineRule("\"", "\"", string, '\\'));

		// special case word rule.
		EmptyCommentRule wordRule = new EmptyCommentRule(multiLineComment);
		rules.add(wordRule);

		// multi-line comments
		rules.add(new MultiLineRule("/*", "*/", multiLineComment));

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}

	public String[] getContentTypes()
	{
		return new String[] {SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT,
				STRING };
	}

	public RebecaTextAttribute[] getContentTypeAttributes()
	{
		// must be same sequence with getContentTypes
		return new RebecaTextAttribute[] { RebecaTextAttribute.SINGLE_LINE_COMMENT,
				RebecaTextAttribute.MULTI_LINE_COMMENT, RebecaTextAttribute.STRING };
	}

	/**
	 * Word rule for empty comments.
	 */
	private static class EmptyCommentRule extends WordRule implements IPredicateRule
	{
		private IToken successToken;

		public EmptyCommentRule(IToken successToken)
		{
			super(new EmptyCommentDetector());
			this.successToken = successToken;
			addWord("/**/", this.successToken);
		}

		public IToken evaluate(ICharacterScanner scanner, boolean resume)
		{
			return evaluate(scanner);
		}

		public IToken getSuccessToken()
		{
			return successToken;
		}

		/**
		 * Detector for empty comments.
		 */
		private static class EmptyCommentDetector implements IWordDetector
		{
			public boolean isWordStart(char c)
			{
				return (c == '/');
			}

			public boolean isWordPart(char c)
			{
				return (c == '*' || c == '/');
			}
		}

	}
}
