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
        
        // Ultra-conservative approach: only fix indentation, preserve everything else
        String formatted = content;
        
        // Step 1: Normalize line endings and remove trailing spaces
        formatted = formatted.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        formatted = formatted.replaceAll("[ \t]+\n", "\n");
        
        // Step 2: Only fix indentation
        formatted = fixIndentationPreservingStructure(formatted);
        
        return formatted;
    }
    
    private String fixIndentationPreservingStructure(String content) {
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
            
            // Add proper indentation for all lines (including comments)
            for (int i = 0; i < indentLevel; i++) {
                result.append(INDENT);
            }
            
            // Apply minimal formatting only to non-comment lines
            String formattedLine = trimmed;
            if (!trimmed.startsWith("/*") && !trimmed.startsWith("//")) {
                formattedLine = applyMinimalFormatting(trimmed);
            }
            
            result.append(formattedLine);
            
            // Increase indent for opening braces
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
            
            result.append(NEW_LINE);
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    private String applyMinimalFormatting(String line) {
        // Apply only the most essential formatting fixes
        
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
        
        return line;
    }
}
