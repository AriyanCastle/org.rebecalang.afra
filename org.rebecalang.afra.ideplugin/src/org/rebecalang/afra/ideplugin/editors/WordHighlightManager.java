package org.rebecalang.afra.ideplugin.editors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Manages word highlighting annotations in the editor.
 * Provides functionality to highlight all occurrences of a selected word.
 */
public class WordHighlightManager {
	
	// Annotation type for word highlighting
	public static final String WORD_HIGHLIGHT_ANNOTATION_TYPE = "org.rebecalang.afra.wordHighlight";
	
	// The source viewer this manager is associated with
	private final ISourceViewer sourceViewer;
	
	// Currently highlighted word and its annotations
	private String currentHighlightedWord;
	private Map<Annotation, Position> currentAnnotations;
	
	/**
	 * Creates a new word highlight manager for the given source viewer.
	 * 
	 * @param sourceViewer The source viewer to manage highlights for
	 */
	public WordHighlightManager(ISourceViewer sourceViewer) {
		this.sourceViewer = sourceViewer;
		this.currentAnnotations = new HashMap<>();
	}
	
	/**
	 * Highlights all occurrences of the word at the given offset.
	 * Clears any previous highlights.
	 * 
	 * @param offset The character offset where the user clicked/positioned cursor
	 */
	public void highlightWordAt(int offset) {
		// Clear previous highlights
		clearHighlights();
		
		if (sourceViewer == null || sourceViewer.getDocument() == null) {
			return;
		}
		
		IDocument document = sourceViewer.getDocument();
		
		// Find the word at the cursor position
		IRegion wordRegion = WordHighlightingUtil.findWordAt(document, offset);
		if (wordRegion == null) {
			return;
		}
		
		// Get the word text
		String word = WordHighlightingUtil.getWordFromRegion(document, wordRegion);
		if (word == null || word.trim().isEmpty()) {
			return;
		}
		
		// Find all occurrences of this word
		List<IRegion> occurrences = WordHighlightingUtil.findAllOccurrences(document, word);
		
		if (!occurrences.isEmpty()) {
			currentHighlightedWord = word;
			highlightRegions(occurrences);
		}
	}
	
	/**
	 * Clears all current word highlights.
	 */
	public void clearHighlights() {
		if (currentAnnotations.isEmpty()) {
			return;
		}
		
		IAnnotationModel annotationModel = getAnnotationModel();
		if (annotationModel != null) {
			// Remove all current annotations
			for (Annotation annotation : currentAnnotations.keySet()) {
				annotationModel.removeAnnotation(annotation);
			}
		}
		
		currentAnnotations.clear();
		currentHighlightedWord = null;
	}
	
	/**
	 * Gets the currently highlighted word.
	 * 
	 * @return The highlighted word, or null if none
	 */
	public String getCurrentHighlightedWord() {
		return currentHighlightedWord;
	}
	
	/**
	 * Highlights the given regions with word highlight annotations.
	 * 
	 * @param regions The regions to highlight
	 */
	private void highlightRegions(List<IRegion> regions) {
		IAnnotationModel annotationModel = getAnnotationModel();
		if (annotationModel == null) {
			return;
		}
		
		currentAnnotations.clear();
		
		for (IRegion region : regions) {
			// Create annotation for this region
			Annotation annotation = new Annotation(WORD_HIGHLIGHT_ANNOTATION_TYPE, false, 
													"Word highlight: " + currentHighlightedWord);
			Position position = new Position(region.getOffset(), region.getLength());
			
			// Add annotation to model
			annotationModel.addAnnotation(annotation, position);
			
			// Keep track of our annotations
			currentAnnotations.put(annotation, position);
		}
	}
	
	/**
	 * Gets the annotation model from the source viewer.
	 * 
	 * @return The annotation model, or null if not available
	 */
	private IAnnotationModel getAnnotationModel() {
		if (sourceViewer == null) {
			return null;
		}
		return sourceViewer.getAnnotationModel();
	}
	
	/**
	 * Disposes of this highlight manager and clears all highlights.
	 */
	public void dispose() {
		clearHighlights();
	}
}
