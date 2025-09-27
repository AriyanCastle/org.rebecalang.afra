package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IFormattingStrategy;

public class PropertyFormattingStrategy implements IFormattingStrategy {
    
    private final PropertyFormatter formatter;
    
    public PropertyFormattingStrategy() {
        this.formatter = new PropertyFormatter();
    }
    
    @Override
    public void formatterStarts(String initialIndentation) {
    }
    
    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        try {
            IDocument tempDoc = new org.eclipse.jface.text.Document(content);
            return formatter.format(tempDoc);
        } catch (Exception e) {
            System.err.println("Formatting failed: " + e.getMessage());
            return content;
        }
    }
    
    @Override
    public void formatterStops() {
    }
    
   
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

    public String formatDocument(IDocument document) {
        try {
            return formatter.format(document);
        } catch (Exception e) {
            System.err.println("Document formatting failed: " + e.getMessage());
            return document.get();
        }
    }
}
