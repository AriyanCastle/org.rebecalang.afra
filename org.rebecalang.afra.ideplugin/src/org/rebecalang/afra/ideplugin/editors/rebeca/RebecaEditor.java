package org.rebecalang.afra.ideplugin.editors.rebeca;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.core.resources.IFile;
import org.rebecalang.afra.ideplugin.editors.ColorManager;

public class RebecaEditor extends TextEditor {

	private static RebecaEditor current;

	private ColorManager colorManager;
	private ProjectionSupport projectionSupport;
	private RealTimeSyntaxChecker syntaxChecker;

	public static RebecaEditor current() {
		return current;
	}

	public RebecaEditor() {
		super();
		current = this;
		colorManager = new ColorManager();
		RebecaTextAttribute.init();
		setDocumentProvider(new RebecaDocumentProvider());
		setSourceViewerConfiguration(new RebecaSourceViewerConfiguration(colorManager, this));
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
	public RebecaSourceViewerConfiguration getPublicSourceViewerConfiguration() {
		return (RebecaSourceViewerConfiguration) getSourceViewerConfiguration();
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        ProjectionViewer viewer =(ProjectionViewer)getSourceViewer();
        
        projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
		projectionSupport.install();
		
		//turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);
		
		annotationModel = viewer.getProjectionAnnotationModel();
		
		// Initialize real-time syntax checker
		initializeRealTimeSyntaxChecker();
		
//		Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
//		while(annotationIterator.hasNext()) {
//			Annotation a = annotationIterator.next();
//			String text = a.getText();
//			System.out.println(text);
//		}
		
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
    
    /**
     * Initialize real-time syntax checker for the current file
     */
    private void initializeRealTimeSyntaxChecker() {
        try {
            System.out.println("RebecaEditor: Initializing real-time syntax checker...");
            
            // Get the file being edited
            IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
            System.out.println("RebecaEditor: File: " + (file != null ? file.getName() : "null"));
            
            if (file != null && "rebeca".equals(file.getFileExtension())) {
                System.out.println("RebecaEditor: Creating RealTimeSyntaxChecker for " + file.getName());
                syntaxChecker = new RealTimeSyntaxChecker(this, file);
                syntaxChecker.startChecking(getDocument());
                System.out.println("RebecaEditor: Real-time syntax checker initialized successfully");
            } else {
                System.out.println("RebecaEditor: Not a .rebeca file, skipping syntax checker initialization");
            }
        } catch (Exception e) {
            System.err.println("RebecaEditor: Failed to initialize real-time syntax checker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void dispose() {
        // Clean up syntax checker
        if (syntaxChecker != null) {
            try {
                syntaxChecker.stopChecking(getDocument());
                syntaxChecker.dispose();
                syntaxChecker = null;
            } catch (Exception e) {
                System.err.println("Error disposing syntax checker: " + e.getMessage());
            }
        }
        
        // Clean up color manager
        if (colorManager != null) {
            colorManager.dispose();
        }
        
        super.dispose();
    }
}
