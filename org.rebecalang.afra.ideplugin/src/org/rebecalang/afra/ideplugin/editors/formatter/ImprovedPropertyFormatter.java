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
        
        // Conservative approach: only fix obvious spacing issues
        String formatted = content;
        
        // Step 1: Normalize line endings and remove trailing spaces
        formatted = formatted.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        formatted = formatted.replaceAll("[ \t]+\n", "\n");
        
        // Step 2: Apply minimal formatting that matches samples exactly
        formatted = applyMinimalFormatting(formatted);
        
        // Step 3: Fix indentation with tabs
        formatted = fixIndentation(formatted);
        
        return formatted;
    }
    
    private String applyMinimalFormatting(String content) {
        // Only fix the most obvious spacing issues without breaking anything
        
        // Fix space around = in assignments
        content = content.replaceAll("([^=!<>])=([^=])", "$1 = $2");
        
        // Fix space around logical operators (be very careful)
        content = content.replaceAll("([^&])&&([^&])", "$1 && $2");
        content = content.replaceAll("([^|])\\|\\|([^|])", "$1 || $2");
        
        // Fix space after colons in property definitions
        content = content.replaceAll(":([^\\s])", ": $1");
        
        // Fix space before { in declarations
        content = content.replaceAll("([a-zA-Z)])\\{", "$1 {");
        
        return content;
    }
    
    private String fixIndentation(String content) {
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
            
            result.append(trimmed);
            
            // Increase indent for opening braces
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
            
            result.append(NEW_LINE);
        }
        
        // Clean up any trailing newlines
        return result.toString().replaceAll("\n+$", "\n");
    }
}
