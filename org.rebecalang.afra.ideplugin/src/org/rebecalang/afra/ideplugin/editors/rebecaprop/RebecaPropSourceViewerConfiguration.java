package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.rebecalang.afra.ideplugin.editors.ColorManager;
import org.rebecalang.afra.ideplugin.editors.GeneralSourceViewerConfiguration;
import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;
import org.rebecalang.afra.ideplugin.editors.formatter.PropertyFormattingStrategy;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;



public class RebecaPropSourceViewerConfiguration extends GeneralSourceViewerConfiguration {

	private RebecaPropEditor editor;

	public RebecaPropSourceViewerConfiguration(ColorManager colorManager, RebecaPropEditor editor) {
		super(colorManager);
		this.editor = editor;
	}

	@Override
	public RuleBasedScanner createScanner() {
		return new RebecaPropScanner(colorManager);
	}

	@Override
	protected GeneralTextAttribute[] getContentTypeAttributes() {
		return new RebecaPropPartitionScanner().getContentTypeAttributes();
	}

	@Override
	public String[] getContentTypes() {
		return new RebecaPropPartitionScanner().getContentTypes();
	}

	@Override
	protected RuleBasedScanner getScanner() {
		if (scanner == null)
		{
			scanner = createScanner();
			scanner.setDefaultReturnToken(new Token(
					RebecaPropTextAttribute.DEFAULT.getTextAttribute(colorManager)));
		}
		return scanner;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sv) {
		ContentAssistant contentAssist = new ContentAssistant();
		IContentAssistProcessor cap = new RebecaPropCompletionProcessor(this.editor);
		contentAssist.setContentAssistProcessor(cap, IDocument.DEFAULT_CONTENT_TYPE);
		
		// Configure content assist behavior
		contentAssist.enableAutoActivation(true);
		contentAssist.setAutoActivationDelay(100); // Quick activation
		contentAssist.enableColoredLabels(true);
		contentAssist.enableAutoInsert(false); // Don't auto-insert, let user choose
		contentAssist.setProposalSelectorBackground(sv.getTextWidget().getDisplay().getSystemColor(org.eclipse.swt.SWT.COLOR_INFO_BACKGROUND));
		contentAssist.setProposalSelectorForeground(sv.getTextWidget().getDisplay().getSystemColor(org.eclipse.swt.SWT.COLOR_INFO_FOREGROUND));
		contentAssist.setInformationControlCreator(getInformationControlCreator(sv));
		
		return contentAssist;
	}

	@Override
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		ContentFormatter formatter = new ContentFormatter();
		PropertyFormattingStrategy formattingStrategy = new PropertyFormattingStrategy();
		
		formatter.setFormattingStrategy(formattingStrategy, IDocument.DEFAULT_CONTENT_TYPE);
		
		return formatter;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        RebecaPropReconcilingStrategy strategy = new RebecaPropReconcilingStrategy();
        strategy.setEditor(this.editor);
        
        MonoReconciler reconciler = new MonoReconciler(strategy,false);
        
        return reconciler;
    }
	
}
