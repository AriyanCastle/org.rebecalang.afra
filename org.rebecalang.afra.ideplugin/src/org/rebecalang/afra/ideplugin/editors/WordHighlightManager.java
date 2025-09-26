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
import org.rebecalang.afra.ideplugin.editors.WordHighlightingUtil.FileType;

/**
 * Manages word highlighting annotations in the editor.
 * Provides functionality to highlight all occurrences of a selected word.
 */
public class WordHighlightManager {
	
	public static final String WORD_HIGHLIGHT_ANNOTATION_TYPE = "org.rebecalang.afra.wordHighlight";
	
	private final ISourceViewer sourceViewer;
	private final FileType fileType;
	private String currentHighlightedWord;
	private Map<Annotation, Position> currentAnnotations;
	
	public WordHighlightManager(ISourceViewer sourceViewer, FileType fileType) {
		this.sourceViewer = sourceViewer;
		this.fileType = fileType;
		this.currentAnnotations = new HashMap<>();
	}
	
	public void highlightWordAt(int offset) {
		clearHighlights();
		
		if (sourceViewer == null || sourceViewer.getDocument() == null) {
			return;
		}
		
		IDocument document = sourceViewer.getDocument();
		
		IRegion wordRegion = WordHighlightingUtil.findWordAt(document, offset, fileType);
		if (wordRegion == null) {
			return;
		}
		
		String word = WordHighlightingUtil.getWordFromRegion(document, wordRegion);
		if (word == null || word.trim().isEmpty()) {
			return;
		}
		
		List<IRegion> occurrences = WordHighlightingUtil.findAllOccurrences(document, word, fileType);
		
		if (!occurrences.isEmpty()) {
			currentHighlightedWord = word;
			highlightRegions(occurrences);
		}
	}
	
	public void clearHighlights() {
		if (currentAnnotations.isEmpty()) {
			return;
		}
		
		IAnnotationModel annotationModel = getAnnotationModel();
		if (annotationModel != null) {
			for (Annotation annotation : currentAnnotations.keySet()) {
				annotationModel.removeAnnotation(annotation);
			}
		}
		
		currentAnnotations.clear();
		currentHighlightedWord = null;
	}
	
	public String getCurrentHighlightedWord() {
		return currentHighlightedWord;
	}
	
	private void highlightRegions(List<IRegion> regions) {
		IAnnotationModel annotationModel = getAnnotationModel();
		if (annotationModel == null) {
			return;
		}
		
		currentAnnotations.clear();
		
		for (IRegion region : regions) {
			Annotation annotation = new Annotation(WORD_HIGHLIGHT_ANNOTATION_TYPE, false, 
													"Word highlight: " + currentHighlightedWord);
			Position position = new Position(region.getOffset(), region.getLength());
			
			annotationModel.addAnnotation(annotation, position);
			currentAnnotations.put(annotation, position);
		}
	}
	
	private IAnnotationModel getAnnotationModel() {
		if (sourceViewer == null) {
			return null;
		}
		return sourceViewer.getAnnotationModel();
	}
	
	public void dispose() {
		clearHighlights();
	}
}
