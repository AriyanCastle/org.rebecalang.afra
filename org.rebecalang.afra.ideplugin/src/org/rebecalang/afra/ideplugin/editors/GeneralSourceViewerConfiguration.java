package org.rebecalang.afra.ideplugin.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;


public abstract class GeneralSourceViewerConfiguration extends SourceViewerConfiguration {
	protected ColorManager colorManager;
	protected RuleBasedScanner scanner;

	public GeneralSourceViewerConfiguration(ColorManager colorManager)
	{
		this.colorManager = colorManager;
	}

	public ColorManager getColorManager()
	{
		return colorManager;
	}

	public abstract String[] getContentTypes();
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
	{
		String[] types = getContentTypes();
		String[] ret = new String[types.length+1];
		ret[0] = IDocument.DEFAULT_CONTENT_TYPE; 
		for( int i = 0 ; i < types.length ; i++ )
			ret[i+1] = types[i];
		return ret;
	}
	
	protected abstract RuleBasedScanner getScanner();

	public abstract RuleBasedScanner createScanner();

	public IPresentationReconciler getPresentationReconciler(
			ISourceViewer sourceViewer)
	{
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr;
		dr = new DefaultDamagerRepairer(getScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		String[] contentTypes = getContentTypes();
		GeneralTextAttribute[] attrs = getContentTypeAttributes();
		for (int i = 0; i < contentTypes.length; i++)
		{
			addRepairer(reconciler, attrs[i], contentTypes[i]);
		}
		return reconciler;
	}

	protected abstract GeneralTextAttribute[] getContentTypeAttributes();

	private void addRepairer(PresentationReconciler reconciler,
			GeneralTextAttribute attr, String partition)
	{
		NonRuleBasedDamagerRepairer ndr;
		ndr = new NonRuleBasedDamagerRepairer(attr
				.getTextAttribute(colorManager));
		reconciler.setDamager(ndr, partition);
		reconciler.setRepairer(ndr, partition);
	}
}
