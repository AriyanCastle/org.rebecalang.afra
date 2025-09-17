package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.rebecalang.afra.ideplugin.editors.ColorManager;
import org.rebecalang.afra.ideplugin.editors.GeneralSourceViewerConfiguration;
import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;
import org.rebecalang.afra.ideplugin.editors.formatter.RebecaFormattingStrategy;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.ITextHover;


public class RebecaSourceViewerConfiguration extends GeneralSourceViewerConfiguration {

	private RebecaEditor editor;

	public RebecaSourceViewerConfiguration(ColorManager cm, RebecaEditor editor) {
		super(cm);
		this.editor = editor;
	}

	@Override
	public RuleBasedScanner createScanner() {
		return new RebecaScanner(colorManager);
	}

	@Override
	protected GeneralTextAttribute[] getContentTypeAttributes() {
		return new RebecaPartitionScanner().getContentTypeAttributes();
	}

	@Override
	public String[] getContentTypes() {
		return new RebecaPartitionScanner().getContentTypes();
	}

	@Override
	protected RuleBasedScanner getScanner() {
		if (scanner == null)
		{
			scanner = createScanner();
			scanner.setDefaultReturnToken(new Token(
					RebecaTextAttribute.DEFAULT.getTextAttribute(colorManager)));
		}
		return scanner;
	}
	
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sv) {
		ContentAssistant contentAssist = new ContentAssistant();
		IContentAssistProcessor cap = new CompletionProcessor(this.editor);
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
       RebecaFormattingStrategy formattingStrategy = new RebecaFormattingStrategy();
       
       formatter.setFormattingStrategy(formattingStrategy, IDocument.DEFAULT_CONTENT_TYPE);
       
       return formatter;
   }

   public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        RebecaReconcilingStrategy strategy = new RebecaReconcilingStrategy();
        strategy.setEditor(this.editor);
        
        MonoReconciler reconciler = new MonoReconciler(strategy,false);
        
        return reconciler;
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
            return new RebecaTextHover(this.editor);
        }
        return super.getTextHover(sourceViewer, contentType);
    }
	
}
