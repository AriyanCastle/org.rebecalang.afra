package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Interface for Afra language formatters
 */
public interface IAfraFormatter {
    
    /**
     * Format the entire document
     * @param document The document to format
     * @return The formatted text
     */
    String format(IDocument document);
    
    /**
     * Format a specific region of the document
     * @param document The document containing the region
     * @param region The region to format
     * @return The formatted text for the region
     */
    String format(IDocument document, IRegion region);
    
    /**
     * Get the indentation string (tabs or spaces)
     * @return The indentation string
     */
    String getIndentString();
}
