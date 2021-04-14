package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.rebecalang.afra.ideplugin.editors.ColorManager;
import org.rebecalang.afra.ideplugin.editors.GeneralSourceViewerConfiguration;
import org.rebecalang.afra.ideplugin.editors.GeneralTextAttribute;
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

	public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        RebecaPropReconcilingStrategy strategy = new RebecaPropReconcilingStrategy();
        strategy.setEditor(this.editor);
        
        MonoReconciler reconciler = new MonoReconciler(strategy,false);
        
        return reconciler;
    }
	
}
