package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Formatter for Rebeca (.rebeca) files
 */
public class RebecaFormatter implements IAfraFormatter {
    
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
        
        // Step 1: Normalize whitespace and remove extra blank lines
        String normalized = normalizeWhitespace(content);
        
        // Step 2: Format operators and keywords
        String formatted = formatOperatorsAndKeywords(normalized);
        
        // Step 3: Format braces and indentation
        String indented = formatBracesAndIndentation(formatted);
        
        // Step 4: Clean up final formatting
        return cleanupFormatting(indented);
    }
    
    private String normalizeWhitespace(String content) {
        // Remove trailing whitespace from each line
        content = content.replaceAll("[ \t]+$", "");
        
        // Normalize line endings
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        // Remove multiple consecutive blank lines (keep maximum 1 blank line)
        content = content.replaceAll("\n{3,}", "\n\n");
        
        return content;
    }
    
    private String formatOperatorsAndKeywords(String content) {
        // Format compound operators FIRST to avoid breaking them apart
        content = content.replaceAll("\\s*==\\s*", " == ");
        content = content.replaceAll("\\s*!=\\s*", " != ");
        content = content.replaceAll("\\s*<=\\s*", " <= ");
        content = content.replaceAll("\\s*>=\\s*", " >= ");
        content = content.replaceAll("\\s*\\+\\+", "++");
        content = content.replaceAll("\\s*--", "--");
        content = content.replaceAll("\\s*&&\\s*", " && ");
        content = content.replaceAll("\\s*\\|\\|\\s*", " || ");
        
        // Format single operators (after compound ones are protected)
        content = content.replaceAll("(?<!\\+|=|!|<|>)\\s*=\\s*(?!=)", " = ");
        content = content.replaceAll("(?<!<|>|=)\\s*<\\s*(?!=)", " < ");
        content = content.replaceAll("(?<!<|>|=)\\s*>\\s*(?!=)", " > ");
        
        // Format arithmetic operators (be careful not to break ++ and --)
        content = content.replaceAll("(?<!\\+)\\s*\\+\\s*(?!\\+)", " + ");
        content = content.replaceAll("(?<!-)\\s*-\\s*(?!-)", " - ");
        content = content.replaceAll("\\s*\\*\\s*", " * ");
        content = content.replaceAll("\\s*/\\s*", " / ");
        content = content.replaceAll("\\s*%\\s*", " % ");
        
        // Format commas in parameter lists
        content = content.replaceAll(",\\s*", ", ");
        
        // Format semicolons
        content = content.replaceAll("\\s*;", ";");
        
        // Format reactiveclass declarations
        content = content.replaceAll("reactiveclass\\s+", "reactiveclass ");
        
        // Format msgsrv declarations
        content = content.replaceAll("msgsrv\\s+", "msgsrv ");
        
        // Format control structures (if, else, for, while) - keep original spacing from samples
        content = content.replaceAll("\\bif\\s*\\(", "if(");
        content = content.replaceAll("\\bfor\\s*\\(", "for(");
        content = content.replaceAll("\\bwhile\\s*\\(", "while(");
        
        return content;
    }
    
    private String formatBracesAndIndentation(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        int indentLevel = 0;
        boolean previousLineWasEmpty = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Skip multiple consecutive empty lines
            if (line.isEmpty()) {
                if (!previousLineWasEmpty && result.length() > 0) {
                    result.append(NEW_LINE);
                    previousLineWasEmpty = true;
                }
                continue;
            }
            previousLineWasEmpty = false;
            
            // Decrease indent for closing braces
            if (line.startsWith("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            // Add proper indentation
            for (int j = 0; j < indentLevel; j++) {
                result.append(INDENT);
            }
            
            // Format the line with proper spacing around braces
            line = formatBracesInLine(line);
            result.append(line);
            
            // Increase indent for opening braces (but only if the line ends with {)
            if (line.endsWith("{")) {
                indentLevel++;
            }
            
            // Add newline after each line
            if (i < lines.length - 1 || !line.isEmpty()) {
                result.append(NEW_LINE);
            }
        }
        
        return result.toString();
    }
    
    private String formatBracesInLine(String line) {
        // Handle constructor parameter syntax :() first
        line = line.replaceAll("\\)\\s*:\\s*\\(", "):(");
        
        // Ensure space before opening braces (but not at start of line)
        line = line.replaceAll("([^\\s])\\s*\\{", "$1 {");
        
        // Ensure proper spacing in method calls and declarations
        line = line.replaceAll("\\)\\s*\\{", ") {");
        
        return line;
    }
    
    private String cleanupFormatting(String content) {
        // Remove any trailing whitespace from lines
        content = content.replaceAll("[ \t]+\n", "\n");
        
        // Ensure file ends with a single newline
        content = content.replaceAll("\n*$", "\n");
        
        // Fix spacing around parentheses (but preserve method calls)
        content = content.replaceAll("\\(\\s+", "(");
        content = content.replaceAll("\\s+\\)", ")");
        
        // Fix any issues with compound operators that might have been broken
        content = content.replaceAll("= = ", "== ");
        content = content.replaceAll("! = ", "!= ");
        content = content.replaceAll("< = ", "<= ");
        content = content.replaceAll("> = ", ">= ");
        content = content.replaceAll("\\+ \\+", "++");
        content = content.replaceAll("- -", "--");
        content = content.replaceAll("& & ", "&& ");
        content = content.replaceAll("\\| \\| ", "|| ");
        
        return content;
    }
}
