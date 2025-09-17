package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Improved formatter for Rebeca (.rebeca) files that matches sample formatting exactly
 */
public class ImprovedRebecaFormatter implements IAfraFormatter {
    
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
        
        // Conservative approach: only fix obvious spacing issues
        String formatted = content;
        
        // Step 1: Normalize line endings and remove trailing spaces
        formatted = formatted.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        formatted = formatted.replaceAll("[ \t]+\n", "\n");
        
        // Step 2: Apply minimal formatting that matches samples exactly
        formatted = applyMinimalFormatting(formatted);
        
        // Step 3: Fix indentation
        formatted = fixIndentation(formatted);
        
        return formatted;
    }
    
    private String applyMinimalFormatting(String content) {
        // Only fix the most obvious spacing issues without breaking anything
        
        // Fix space after commas in parameter lists
        content = content.replaceAll(",([^\\s])", ", $1");
        
        // Fix space around = (but be very careful)
        content = content.replaceAll("([^=!<>])=([^=])", "$1 = $2");
        
        // Fix space before { in declarations (but preserve existing good spacing)
        content = content.replaceAll("([a-zA-Z)])\\{", "$1 {");
        
        // Ensure proper spacing in if statements
        content = content.replaceAll("if\\(", "if(");
        content = content.replaceAll("else\\{", "} else {");
        
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
            
            // Add indentation
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
