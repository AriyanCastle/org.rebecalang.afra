package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IFormattingStrategy;

/**
 * Formatting strategy for Rebeca files
 */
public class RebecaFormattingStrategy implements IFormattingStrategy {
    
    private final FixedRebecaFormatter formatter;
    
    public RebecaFormattingStrategy() {
        this.formatter = new FixedRebecaFormatter();
    }
    
    @Override
    public void formatterStarts(String initialIndentation) {
        // No initialization needed
    }
    
    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        try {
            // Create a temporary document to use the formatter
            IDocument tempDoc = new org.eclipse.jface.text.Document(content);
            return formatter.format(tempDoc);
        } catch (Exception e) {
            // If formatting fails, return original content
            System.err.println("Formatting failed: " + e.getMessage());
            return content;
        }
    }
    
    @Override
    public void formatterStops() {
        // No cleanup needed
    }
    
    /**
     * Format a specific region of a document
     */
    public String formatRegion(IDocument document, IRegion region) {
        try {
            return formatter.format(document, region);
        } catch (Exception e) {
            System.err.println("Region formatting failed: " + e.getMessage());
            try {
                return document.get(region.getOffset(), region.getLength());
            } catch (BadLocationException ex) {
                return "";
            }
        }
    }
    
    /**
     * Format the entire document
     */
    public String formatDocument(IDocument document) {
        try {
            return formatter.format(document);
        } catch (Exception e) {
            System.err.println("Document formatting failed: " + e.getMessage());
            return document.get();
        }
    }
}
