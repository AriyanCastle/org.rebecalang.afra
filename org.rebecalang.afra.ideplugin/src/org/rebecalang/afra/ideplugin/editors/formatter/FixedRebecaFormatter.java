package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Fixed formatter for Rebeca (.rebeca) files that correctly handles comments
 */
public class FixedRebecaFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String format(IDocument document, IRegion region) {
        try {
            String content = document.get(region.getOffset(), region.getLength());
            return formatContent(content);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String getIndentString() {
        return INDENT;
    }
    
    private String formatContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Normalize line endings and remove trailing spaces
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        content = content.replaceAll("[ \t]+\n", "\n");
        
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int braceLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Handle empty lines
            if (trimmed.isEmpty()) {
                result.append(NEW_LINE);
                continue;
            }
            
            // Calculate indentation level for this line
            int currentIndent = braceLevel;
            
            // Closing braces decrease the indentation level for themselves
            if (trimmed.startsWith("}")) {
                currentIndent = Math.max(0, braceLevel - 1);
                braceLevel = currentIndent; // Update for next lines
            }
            
            // Apply indentation
            for (int i = 0; i < currentIndent; i++) {
                result.append(INDENT);
            }
            
            // Add the line content
            result.append(trimmed);
            result.append(NEW_LINE);
            
            // Opening braces increase indentation level for subsequent lines
            if (trimmed.endsWith("{")) {
                braceLevel++;
            }
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
}
