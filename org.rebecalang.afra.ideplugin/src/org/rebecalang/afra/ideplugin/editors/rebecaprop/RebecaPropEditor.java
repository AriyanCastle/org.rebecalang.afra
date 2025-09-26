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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.rebecalang.afra.ideplugin.editors.WordHighlightManager;
import org.rebecalang.afra.ideplugin.editors.WordHighlightingUtil;

public class RebecaPropEditor extends TextEditor {
	
	private static RebecaPropEditor current;

	private ColorManager colorManager;
	private ProjectionSupport projectionSupport;
	private WordHighlightManager wordHighlightManager;

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

	public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        ProjectionViewer viewer =(ProjectionViewer)getSourceViewer();
        
        projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
		projectionSupport.install();
		
		viewer.doOperation(ProjectionViewer.TOGGLE);
		
		annotationModel = viewer.getProjectionAnnotationModel();
		
		wordHighlightManager = new WordHighlightManager(viewer, WordHighlightingUtil.FileType.PROPERTY);
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
     * Sets up word highlighting functionality by adding mouse listeners to the text widget.
     */
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
    	super.dispose();
    }
}
