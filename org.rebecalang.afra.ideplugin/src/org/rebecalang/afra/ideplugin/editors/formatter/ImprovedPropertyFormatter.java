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
        
        String[] lines = content.split("\n");
        
        // First pass: Calculate indentation levels ignoring comments completely
        int[] indentLevels = calculateIndentationLevels(lines);
        
        // Second pass: Apply indentation and formatting
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            
            if (trimmed.isEmpty()) {
                result.append(NEW_LINE);
                continue;
            }
            
            // Add indentation using tabs
            for (int j = 0; j < indentLevels[i]; j++) {
                result.append(INDENT);
            }
            
            // Apply formatting based on line type
            if (isComment(trimmed)) {
                // For comments, just use the trimmed content without additional formatting
                result.append(trimmed);
            } else {
                // For non-comment lines, apply essential formatting
                trimmed = applyEssentialFormatting(trimmed);
                result.append(trimmed);
            }
            
            result.append(NEW_LINE);
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    private int[] calculateIndentationLevels(String[] lines) {
        int[] indentLevels = new int[lines.length];
        int currentIndentLevel = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            
            if (trimmed.isEmpty() || isComment(trimmed)) {
                // Comments and empty lines inherit the current indentation level
                indentLevels[i] = currentIndentLevel;
                continue;
            }
            
            // Adjust for closing braces BEFORE setting the indentation for this line
            if (trimmed.startsWith("}")) {
                currentIndentLevel = Math.max(0, currentIndentLevel - 1);
            }
            
            // Set the indentation level for this line
            indentLevels[i] = currentIndentLevel;
            
            // Adjust for opening braces AFTER setting the indentation for this line
            if (trimmed.endsWith("{")) {
                currentIndentLevel++;
            }
        }
        
        return indentLevels;
    }
    
    private boolean isComment(String trimmed) {
        return trimmed.startsWith("//") || trimmed.startsWith("/*") || 
               trimmed.startsWith("*") || trimmed.endsWith("*/");
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
