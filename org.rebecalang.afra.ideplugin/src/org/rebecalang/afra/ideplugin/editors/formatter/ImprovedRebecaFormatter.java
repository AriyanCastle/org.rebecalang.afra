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
            
            // Add proper indentation for all lines (including comments)
            for (int j = 0; j < indentLevels[i]; j++) {
                result.append(INDENT);
            }
            
            // Apply formatting based on line type
            String formattedLine = trimmed;
            if (isComment(trimmed)) {
                // For comments, just use the trimmed content without additional formatting
                formattedLine = trimmed;
            } else {
                // For non-comment lines, apply minimal formatting
                formattedLine = applyMinimalFormatting(trimmed);
            }
            
            result.append(formattedLine);
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
