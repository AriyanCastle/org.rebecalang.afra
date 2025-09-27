package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextEditor;
import org.rebecalang.afra.ideplugin.editors.ColorManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.core.resources.IFile;

public class RebecaPropEditor extends TextEditor {
	
	private static RebecaPropEditor current;

	private ColorManager colorManager;
	private ProjectionSupport projectionSupport;
	private PropertyRealTimeSyntaxChecker syntaxChecker;

	public static RebecaPropEditor current() {
		return current;
	}

	public RebecaPropEditor() {
		super();
		current = this;
		colorManager = new ColorManager();
		RebecaPropTextAttribute.init();
		setDocumentProvider(new RebecaPropDocumentProvider());
		setSourceViewerConfiguration(new RebecaPropSourceViewerConfiguration(colorManager, this));
	}

	public IDocument getDocument() {
		return getDocumentProvider().getDocument(getEditorInput());
	}

	protected void initializeEditor() {
		super.initializeEditor();
	}

	/**
	 * @return Returns the colorManager.
	 */
	public ColorManager getColorManager() {
		return colorManager;
	}
	
	/**
	 * Public accessor for the source viewer (exposes protected method)
	 */
	public ISourceViewer getPublicSourceViewer() {
		return getSourceViewer();
	}
	
	/**
	 * Public accessor for the source viewer configuration (exposes protected method)
	 */
	public RebecaPropSourceViewerConfiguration getPublicSourceViewerConfiguration() {
		return (RebecaPropSourceViewerConfiguration) getSourceViewerConfiguration();
	}

	public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        ProjectionViewer viewer =(ProjectionViewer)getSourceViewer();
        
        projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
		projectionSupport.install();
		
		//turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);
		
		annotationModel = viewer.getProjectionAnnotationModel();
		
		initializeRealTimeSyntaxChecker();
		
    }
	private Annotation[] oldAnnotations;
	private ProjectionAnnotationModel annotationModel;
	
	public void updateFoldingStructure(ArrayList<Position> positions)
	{
		Annotation[] annotations = new Annotation[positions.size()];
		
		//this will hold the new annotations along
		//with their corresponding positions
		HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		
		for(int i =0;i<positions.size();i++) {
			ProjectionAnnotation annotation = new ProjectionAnnotation();
			
			newAnnotations.put(annotation, positions.get(i));
			
			annotations[i]=annotation;
		}
		
		annotationModel.modifyAnnotations(oldAnnotations,newAnnotations,null);
		
		oldAnnotations=annotations;
	}
	
    
    protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles)
    {
        ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

    	// ensure decoration support has been created and configured.
    	getSourceViewerDecorationSupport(viewer);
    	
	    	return viewer;
    }
    

    private void initializeRealTimeSyntaxChecker() {
        try {
            
            IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
            
            if (file != null && "property".equals(file.getFileExtension())) {
                syntaxChecker = new PropertyRealTimeSyntaxChecker(this, file);
                syntaxChecker.startChecking(getDocument());
            }
        } catch (Exception e) {
            System.err.println("RebecaPropEditor: Failed to initialize real-time syntax checker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void dispose() {
        if (syntaxChecker != null) {
            try {
                syntaxChecker.stopChecking(getDocument());
                syntaxChecker.dispose();
                syntaxChecker = null;
            } catch (Exception e) {
                System.err.println("Error disposing syntax checker: " + e.getMessage());
            }
        }
        
        if (colorManager != null) {
            colorManager.dispose();
        }
        
        super.dispose();
    }
}
