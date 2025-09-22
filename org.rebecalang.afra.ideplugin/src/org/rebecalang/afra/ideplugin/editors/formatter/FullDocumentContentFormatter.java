package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IFormattingStrategy;

/**
 * A custom content formatter that formats the entire document at once,
 * bypassing Eclipse's token-based chunking behavior
 */
public class FullDocumentContentFormatter implements IContentFormatter {
    
    private final IAfraFormatter formatter;
    
    public FullDocumentContentFormatter(IAfraFormatter formatter) {
        this.formatter = formatter;
    }
    
    @Override
    public void format(IDocument document, IRegion region) {
        System.out.println("FullDocumentContentFormatter.format called for region");
        try {
            if (region.getOffset() == 0 && region.getLength() == document.getLength()) {
                // Format entire document
                System.out.println("Formatting entire document, length: " + document.getLength());
                String originalContent = document.get();
                System.out.println("Original content: [" + originalContent + "]");
                String formattedContent = formatter.format(document);
                System.out.println("Formatted content: [" + formattedContent + "]");
                document.replace(0, document.getLength(), formattedContent);
            } else {
                // Format only the specified region
                System.out.println("Formatting region: offset=" + region.getOffset() + ", length=" + region.getLength());
                String formattedContent = formatter.format(document, region);
                document.replace(region.getOffset(), region.getLength(), formattedContent);
            }
        } catch (BadLocationException e) {
            System.err.println("Error formatting document: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public IFormattingStrategy getFormattingStrategy(String contentType) {
        // Not used in our implementation since we handle formatting directly
        return null;
    }
}
