package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IFormattingStrategy;

/**
 * AST-based formatting strategy for Property files
 */
public class ASTPropertyFormattingStrategy implements IFormattingStrategy {
    
    private final ASTPropertyFormatter formatter;
    
    public ASTPropertyFormattingStrategy() {
        this.formatter = new ASTPropertyFormatter();
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
            String result = formatter.format(tempDoc);
            System.out.println("ASTPropertyFormattingStrategy: Successfully formatted content");
            return result;
        } catch (Exception e) {
            // If AST formatting fails, return original content
            System.err.println("AST Property formatting failed, returning original content: " + e.getMessage());
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
            String result = formatter.format(document, region);
            System.out.println("ASTPropertyFormattingStrategy: Successfully formatted region");
            return result;
        } catch (Exception e) {
            System.err.println("AST Property region formatting failed: " + e.getMessage());
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
            String result = formatter.format(document);
            System.out.println("ASTPropertyFormattingStrategy: Successfully formatted document");
            return result;
        } catch (Exception e) {
            System.err.println("AST Property document formatting failed: " + e.getMessage());
            return document.get();
        }
    }
}
