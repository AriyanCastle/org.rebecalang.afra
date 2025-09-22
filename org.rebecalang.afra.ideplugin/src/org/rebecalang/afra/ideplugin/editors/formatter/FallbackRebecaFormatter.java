package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Fallback formatter for Rebeca files that provides good formatting without relying on specific AST classes.
 * This formatter uses pattern matching and structural analysis to format Rebeca code properly.
 */
public class FallbackRebecaFormatter implements IAfraFormatter {
    
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
            try {
                return document.get();
            } catch (Exception ex) {
                return "";
            }
        }
    }
    
    @Override
    public String getIndentString() {
        return INDENT;
    }
    
    /**
     * Main formatting method
     */
    private String formatContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Determine if this is a .property file or .rebeca file
        if (isPropertyFile(content)) {
            return formatPropertyFile(content);
        } else {
            return formatRebecaFile(content);
        }
    }
    
    /**
     * Check if content appears to be a property file
     */
    private boolean isPropertyFile(String content) {
        return content.trim().startsWith("property") || 
               content.contains("define {") || 
               content.contains("LTL {") || 
               content.contains("Assertion {");
    }
    
    /**
     * Format Rebeca source files using pattern matching
     */
    private String formatRebecaFile(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\r?\\n");
        
        int indentLevel = 0;
        boolean inClass = false;
        boolean inSection = false;
        boolean inMain = false;
        boolean inComment = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Skip empty lines but preserve them strategically
            if (line.isEmpty()) {
                // Add blank line if we're between major sections
                if (result.length() > 0 && !result.toString().endsWith(NEW_LINE + NEW_LINE)) {
                    result.append(NEW_LINE);
                }
                continue;
            }
            
            // Handle multi-line comments
            if (line.startsWith("/*") && !line.endsWith("*/")) {
                inComment = true;
            }
            if (inComment) {
                appendIndentedLine(result, line, indentLevel);
                if (line.endsWith("*/")) {
                    inComment = false;
                }
                continue;
            }
            
            // Handle single-line comments
            if (line.startsWith("//") || line.startsWith("/*")) {
                appendIndentedLine(result, line, indentLevel);
                continue;
            }
            
            // Check for class declaration
            if (line.startsWith("reactiveclass")) {
                if (inClass) {
                    result.append(NEW_LINE); // Blank line between classes
                }
                appendIndentedLine(result, line, 0);
                inClass = true;
                indentLevel = 1;
                continue;
            }
            
            // Check for main section
            if (line.equals("main {")) {
                if (inClass) {
                    result.append(NEW_LINE); // Blank line before main
                }
                appendIndentedLine(result, line, 0);
                inMain = true;
                indentLevel = 1;
                continue;
            }
            
            // Handle section starts (knownrebecs, statevars)
            if (line.equals("knownrebecs {") || line.equals("statevars {")) {
                if (inSection) {
                    result.append(NEW_LINE); // Blank line between sections
                }
                appendIndentedLine(result, line, 1);
                inSection = true;
                indentLevel = 2;
                continue;
            }
            
            // Handle method declarations
            if (line.startsWith("msgsrv ") || isConstructor(line)) {
                if (inSection) {
                    result.append(NEW_LINE); // Blank line after sections
                    inSection = false;
                }
                appendIndentedLine(result, line, 1);
                indentLevel = 2;
                continue;
            }
            
            // Handle closing braces
            if (line.equals("}")) {
                if (inSection) {
                    indentLevel = 1;
                    inSection = false;
                } else if (inClass) {
                    indentLevel = 0;
                    inClass = false;
                } else if (inMain) {
                    indentLevel = 0;
                    inMain = false;
                } else {
                    indentLevel = Math.max(0, indentLevel - 1);
                }
                appendIndentedLine(result, line, indentLevel);
                continue;
            }
            
            // Handle opening braces on same line
            if (line.endsWith(" {")) {
                appendIndentedLine(result, line, indentLevel);
                indentLevel++;
                continue;
            }
            
            // Format regular statements
            line = formatStatement(line);
            appendIndentedLine(result, line, indentLevel);
        }
        
        return result.toString();
    }
    
    /**
     * Check if line is a constructor declaration
     */
    private boolean isConstructor(String line) {
        // Constructor pattern: ClassName(params) {
        Pattern constructorPattern = Pattern.compile("^[A-Z]\\w*\\s*\\([^)]*\\)\\s*\\{?");
        return constructorPattern.matcher(line).find();
    }
    
    /**
     * Format individual statements
     */
    private String formatStatement(String line) {
        // Normalize spacing around operators
        line = line.replaceAll("\\s*=\\s*", " = ");
        line = line.replaceAll("\\s*\\+\\s*", " + ");
        line = line.replaceAll("\\s*-\\s*", " - ");
        line = line.replaceAll("\\s*\\*\\s*", " * ");
        line = line.replaceAll("\\s*/\\s*", " / ");
        line = line.replaceAll("\\s*&&\\s*", " && ");
        line = line.replaceAll("\\s*\\|\\|\\s*", " || ");
        line = line.replaceAll("\\s*==\\s*", " == ");
        line = line.replaceAll("\\s*!=\\s*", " != ");
        line = line.replaceAll("\\s*<=\\s*", " <= ");
        line = line.replaceAll("\\s*>=\\s*", " >= ");
        line = line.replaceAll("\\s*<\\s*", " < ");
        line = line.replaceAll("\\s*>\\s*", " > ");
        
        // Fix spacing around parentheses
        line = line.replaceAll("\\s*\\(\\s*", "(");
        line = line.replaceAll("\\s*\\)\\s*", ")");
        line = line.replaceAll("\\s*,\\s*", ", ");
        
        // Fix spacing around method calls
        line = line.replaceAll("(\\w+)\\s*\\.\\s*(\\w+)", "$1.$2");
        
        return line;
    }
    
    /**
     * Format property files
     */
    private String formatPropertyFile(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\r?\\n");
        
        int indentLevel = 0;
        boolean needsBlankLine = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Property block start
            if (trimmed.startsWith("property")) {
                appendIndentedLine(result, "property {", 0);
                indentLevel = 1;
                continue;
            }
            
            // Section starts
            if (trimmed.startsWith("define") || trimmed.startsWith("Assertion") || trimmed.startsWith("LTL")) {
                if (needsBlankLine) {
                    result.append(NEW_LINE);
                }
                appendIndentedLine(result, trimmed + " {", indentLevel);
                indentLevel++;
                needsBlankLine = false;
                continue;
            }
            
            // Closing braces
            if (trimmed.equals("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
                appendIndentedLine(result, "}", indentLevel);
                needsBlankLine = true;
                continue;
            }
            
            // Regular property lines
            String formattedLine = formatPropertyLine(trimmed);
            appendIndentedLine(result, formattedLine, indentLevel);
        }
        
        return result.toString();
    }
    
    /**
     * Format property line
     */
    private String formatPropertyLine(String line) {
        line = line.replaceAll("\\s*=\\s*", " = ");
        line = line.replaceAll("\\s*:\\s*", ": ");
        line = line.replaceAll("\\s*&&\\s*", " && ");
        line = line.replaceAll("\\s*\\|\\|\\s*", " || ");
        line = line.replaceAll("\\s*!\\s*", "!");
        line = line.replaceAll("\\s*\\(\\s*", "(");
        line = line.replaceAll("\\s*\\)\\s*", ")");
        line = line.replaceAll("\\s*,\\s*", ", ");
        
        if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
            line += ";";
        }
        
        return line;
    }
    
    /**
     * Append line with proper indentation
     */
    private void appendIndentedLine(StringBuilder result, String line, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            result.append(INDENT);
        }
        result.append(line);
        result.append(NEW_LINE);
    }
}
