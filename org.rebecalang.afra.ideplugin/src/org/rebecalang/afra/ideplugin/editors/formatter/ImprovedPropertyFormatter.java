package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Improved formatter for Property (.property) files that uses tabs and matches sample formatting
 */
public class ImprovedPropertyFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t"; // Use tabs like in the samples
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
        
        // Step 1: Just fix indentation and basic structure
        String formatted = fixIndentationOnly(content);
        
        return formatted;
    }
    
    private String fixIndentationOnly(String content) {
        // Normalize line endings and remove trailing spaces
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        content = content.replaceAll("[ \t]+\n", "\n");
        
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        int indentLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                result.append(NEW_LINE);
                continue;
            }
            
            // Decrease indent for closing braces
            if (trimmed.startsWith("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            // Add indentation using tabs
            for (int i = 0; i < indentLevel; i++) {
                result.append(INDENT);
            }
            
            // Apply only essential formatting
            trimmed = applyEssentialFormatting(trimmed);
            result.append(trimmed);
            
            // Increase indent for opening braces
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
            
            result.append(NEW_LINE);
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    private String applyEssentialFormatting(String line) {
        // Only fix the most critical spacing issues
        
        // Fix space after commas (only if missing)
        if (line.contains(",") && !line.matches(".*,\\s.*")) {
            line = line.replaceAll(",([^\\s])", ", $1");
        }
        
        // Fix space before { (only if missing)
        if (line.contains("{") && !line.matches(".*\\s\\{.*")) {
            line = line.replaceAll("([a-zA-Z)])\\{", "$1 {");
        }
        
        // Fix basic assignment spacing (be very conservative)
        // Only fix obvious cases like "a=b" to "a = b"
        line = line.replaceAll("([a-zA-Z0-9])=([a-zA-Z0-9])", "$1 = $2");
        
        // Fix space after colons (only if missing)
        if (line.contains(":") && !line.matches(".*:\\s.*")) {
            line = line.replaceAll(":([^\\s])", ": $1");
        }
        
        return line;
    }
}
