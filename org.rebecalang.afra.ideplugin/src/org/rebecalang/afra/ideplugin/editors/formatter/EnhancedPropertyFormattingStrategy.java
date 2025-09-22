package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IFormattingStrategy;

/**
 * Enhanced formatting strategy for Property files that uses AST-based formatting.
 * This can be used as a drop-in replacement for the existing property formatting strategy.
 */
public class EnhancedPropertyFormattingStrategy implements IFormattingStrategy {
    
    private final IAfraFormatter formatter;
    
    public EnhancedPropertyFormattingStrategy() {
        // Use the registry to get the best available formatter
        this.formatter = FormatterRegistry.getInstance().getPropertyFormatter();
    }
    
    @Override
    public void formatterStarts(String initialIndentation) {
        // No initialization needed for our formatter
    }
    
    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        try {
            // Create a temporary document for formatting
            org.eclipse.jface.text.Document doc = new org.eclipse.jface.text.Document(content);
            return formatter.format(doc);
        } catch (Exception e) {
            System.err.println("Enhanced property formatting failed, returning original content: " + e.getMessage());
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
            System.err.println("Property region formatting failed: " + e.getMessage());
            try {
                return document.get(region.getOffset(), region.getLength());
            } catch (BadLocationException ex) {
                return "";
            }
        }
    }
    
    /**
     * Format an entire document
     */
    public String formatDocument(IDocument document) {
        try {
            return formatter.format(document);
        } catch (Exception e) {
            System.err.println("Property document formatting failed: " + e.getMessage());
            return document.get();
        }
    }
    
    /**
     * Get the indent string used by this formatter
     */
    public String getIndentString() {
        return formatter.getIndentString();
    }
}
