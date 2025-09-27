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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.core.resources.IFile;
import org.rebecalang.afra.ideplugin.editors.ColorManager;
import org.rebecalang.afra.ideplugin.editors.WordHighlightManager;
import org.rebecalang.afra.ideplugin.editors.WordHighlightingUtil;

public class RebecaEditor extends TextEditor {

	private static RebecaEditor current;

	private ColorManager colorManager;
	private ProjectionSupport projectionSupport;
	private RealTimeSyntaxChecker syntaxChecker;

	private WordHighlightManager wordHighlightManager;

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
		
		viewer.doOperation(ProjectionViewer.TOGGLE);
		
		annotationModel = viewer.getProjectionAnnotationModel();
		

		initializeRealTimeSyntaxChecker();
		
//		Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
//		while(annotationIterator.hasNext()) {
//			Annotation a = annotationIterator.next();
//			String text = a.getText();
//			System.out.println(text);
//		}

		wordHighlightManager = new WordHighlightManager(viewer, WordHighlightingUtil.FileType.REBECA);
		setupWordHighlighting(viewer);
		
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
            
            IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
            System.out.println("RebecaEditor: File: " + (file != null ? file.getName() : "null"));
            
            if (file != null && "rebeca".equals(file.getFileExtension())) {
                syntaxChecker = new RealTimeSyntaxChecker(this, file);
                syntaxChecker.startChecking(getDocument());
            } else {
                System.out.println("RebecaEditor: Not a .rebeca file, skipping syntax checker initialization");
            }
        } catch (Exception e) {
            System.err.println("RebecaEditor: Failed to initialize real-time syntax checker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

 
    private void setupWordHighlighting(ISourceViewer viewer) {
    	if (viewer != null && viewer.getTextWidget() != null) {
    		Control textWidget = viewer.getTextWidget();
    		
    		// Add mouse listener for word highlighting on click
    		textWidget.addMouseListener(new MouseListener() {
    			@Override
    			public void mouseUp(MouseEvent e) {
    				// Only handle left mouse button clicks
    				if (e.button == 1) {
    					handleWordHighlighting(viewer, e);
    				}
    			}
    			
    			@Override
    			public void mouseDown(MouseEvent e) {
    				// Not used
    			}
    			
    			@Override
    			public void mouseDoubleClick(MouseEvent e) {
    				// Not used - let the default double-click behavior handle word selection
    			}
    		});
    	}
    }
    
    /**
     * Handles word highlighting when the user clicks in the editor.
     */
    private void handleWordHighlighting(ISourceViewer viewer, MouseEvent e) {
    	if (wordHighlightManager == null) {
    		return;
    	}
    	
    	try {
    		// Convert mouse coordinates to document offset
    		Point point = new Point(e.x, e.y);
    		int offset = viewer.getTextWidget().getOffsetAtLocation(point);
    		
    		// Highlight word at this offset
    		wordHighlightManager.highlightWordAt(offset);
    		
    	} catch (IllegalArgumentException ex) {
    		// Click was outside text area, clear highlights
    		wordHighlightManager.clearHighlights();
    	} catch (Exception ex) {
    		// Handle any other exceptions silently
    		System.err.println("Error in word highlighting: " + ex.getMessage());
    	}
    }
    
    /**
     * Disposes the editor and cleans up resources.
     */
    @Override
    public void dispose() {
    	if (wordHighlightManager != null) {
    		wordHighlightManager.dispose();
    		wordHighlightManager = null;
    	}
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
