package org.rebecalang.afra.ideplugin.editors;

import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;

public abstract class GeneralRuleBasedPartitionScanner extends RuleBasedPartitionScanner {
	public abstract String[] getContentTypes();
	public abstract GeneralTextAttribute[] getContentTypeAttributes();
}
