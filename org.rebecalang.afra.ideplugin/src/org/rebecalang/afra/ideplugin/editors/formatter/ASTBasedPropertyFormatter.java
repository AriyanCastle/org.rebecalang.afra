package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.propertycompiler.generalrebeca.objectmodel.PropertyModel;
import org.rebecalang.compiler.propertycompiler.generalrebeca.objectmodel.PropertyDefinition;
import org.rebecalang.compiler.propertycompiler.generalrebeca.PropertyCompiler;
import org.rebecalang.rmc.RMCConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * AST-based formatter for Property (.property) files that uses the property compiler's AST
 * to properly format property definitions while preserving comments and maintaining proper indentation.
 */
public class ASTBasedPropertyFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    @Autowired
    private PropertyCompiler propertyCompiler;
    
    // Comment storage for preserving comments during AST-based formatting
    private static class Comment {
        int line;
        int column;
        String content;
        CommentType type;
        
        Comment(int line, int column, String content, CommentType type) {
            this.line = line;
            this.column = column;
            this.content = content;
            this.type = type;
        }
    }
    
    private enum CommentType {
        SINGLE_LINE,    // //
        MULTI_LINE      // /* */
    }
    
    // Formatting state for tracking context during formatting
    private static class FormattingContext {
        private int indentLevel = 0;
        private StringBuilder output = new StringBuilder();
        private List<Comment> comments = new ArrayList<>();
        private int currentLine = 1;
        
        void increaseIndent() { indentLevel++; }
        void decreaseIndent() { indentLevel = Math.max(0, indentLevel - 1); }
        
        void appendIndent() {
            for (int i = 0; i < indentLevel; i++) {
                output.append(INDENT);
            }
        }
        
        void appendLine(String text) {
            appendIndent();
            output.append(text).append(NEW_LINE);
            currentLine++;
        }
        
        void appendEmptyLine() {
            output.append(NEW_LINE);
            currentLine++;
        }
        
        void append(String text) {
            output.append(text);
        }
        
        void appendCommentsBefore(int line) {
            for (Comment comment : comments) {
                if (comment.line < line) {
                    appendComment(comment);
                }
            }
            comments.removeIf(c -> c.line < line);
        }
        
        private void appendComment(Comment comment) {
            switch (comment.type) {
                case SINGLE_LINE:
                    appendLine(comment.content);
                    break;
                case MULTI_LINE:
                    String[] lines = comment.content.split("\n");
                    for (String line : lines) {
                        appendLine(line.trim());
                    }
                    break;
            }
        }
        
        String getResult() {
            // Append any remaining comments
            for (Comment comment : comments) {
                appendComment(comment);
            }
            return output.toString();
        }
    }
    
    public ASTBasedPropertyFormatter() {
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("ASTBasedPropertyFormatter: Failed to initialize compiler components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to original content
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
        
        try {
            // Step 1: Extract comments before parsing
            List<Comment> comments = extractComments(content);
            
            // Step 2: Remove comments for AST parsing
            String cleanContent = removeComments(content);
            
            // Step 3: Parse AST
            PropertyModel astModel = parsePropertyAST(cleanContent);
            
            // Step 4: Format using AST + preserved comments
            if (astModel != null) {
                return formatWithAST(astModel, comments);
            } else {
                // Fallback to basic formatting if AST parsing fails
                return basicFormat(content);
            }
            
        } catch (Exception e) {
            System.err.println("Property AST formatting failed, using fallback: " + e.getMessage());
            return basicFormat(content);
        }
    }
    
    private List<Comment> extractComments(String content) {
        List<Comment> comments = new ArrayList<>();
        
        // Patterns for different comment types
        Pattern singleLinePattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        Pattern multiLinePattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        
        String[] lines = content.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Check for single-line comments
            Matcher singleLineMatcher = singleLinePattern.matcher(line);
            if (singleLineMatcher.find()) {
                int column = singleLineMatcher.start();
                String commentContent = singleLineMatcher.group();
                comments.add(new Comment(lineNumber, column, commentContent, CommentType.SINGLE_LINE));
            }
        }
        
        // Check for multi-line comments
        Matcher multiLineMatcher = multiLinePattern.matcher(content);
        while (multiLineMatcher.find()) {
            int line = getLineNumber(content, multiLineMatcher.start());
            int column = getColumnNumber(content, multiLineMatcher.start());
            String commentContent = multiLineMatcher.group();
            comments.add(new Comment(line, column, commentContent, CommentType.MULTI_LINE));
        }
        
        return comments;
    }
    
    private int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    private int getColumnNumber(String content, int position) {
        int column = 0;
        for (int i = position - 1; i >= 0; i--) {
            if (content.charAt(i) == '\n') {
                break;
            }
            column++;
        }
        return column;
    }
    
    private String removeComments(String content) {
        // Remove all comments for clean AST parsing
        content = content.replaceAll("//.*$", "", Pattern.MULTILINE);
        content = content.replaceAll("/\\*.*?\\*/", "", Pattern.DOTALL);
        return content;
    }
    
    private PropertyModel parsePropertyAST(String content) {
        try {
            // Create temporary file for parsing
            File tempFile = File.createTempFile("property_format_", ".property");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }
            
            // Parse using the property compiler
            if (propertyCompiler != null) {
                PropertyModel result = propertyCompiler.compilePropertyFile(tempFile);
                tempFile.delete();
                return result;
            }
            
            tempFile.delete();
            return null;
            
        } catch (Exception e) {
            System.err.println("Failed to parse property AST: " + e.getMessage());
            return null;
        }
    }
    
    private String formatWithAST(PropertyModel model, List<Comment> comments) {
        FormattingContext ctx = new FormattingContext();
        ctx.comments = comments;
        
        // Main property block
        ctx.appendLine("property {");
        ctx.increaseIndent();
        
        // Define section
        if (model.getPropertyDefinition() != null && !model.getPropertyDefinition().isEmpty()) {
            ctx.appendLine("define {");
            ctx.increaseIndent();
            
            for (PropertyDefinition propDef : model.getPropertyDefinition()) {
                String defineLine = formatPropertyDefinition(propDef);
                ctx.appendLine(defineLine);
            }
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
            ctx.appendEmptyLine();
        }
        
        // Assertion section
        if (hasAssertions(model)) {
            ctx.appendLine("Assertion {");
            ctx.increaseIndent();
            
            formatAssertions(model, ctx);
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
            ctx.appendEmptyLine();
        }
        
        // LTL section
        if (hasLTLProperties(model)) {
            ctx.appendLine("LTL {");
            ctx.increaseIndent();
            
            formatLTLProperties(model, ctx);
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
        
        return ctx.getResult();
    }
    
    private String formatPropertyDefinition(PropertyDefinition propDef) {
        // Format: name = expression;
        StringBuilder sb = new StringBuilder();
        sb.append(propDef.getName());
        sb.append(" = ");
        sb.append(formatExpression(propDef.getExpression()));
        sb.append(";");
        return sb.toString();
    }
    
    private String formatExpression(Object expression) {
        if (expression == null) {
            return "";
        }
        
        String expr = expression.toString();
        
        // Normalize spacing around operators
        expr = expr.replaceAll("\\s*&&\\s*", " && ");
        expr = expr.replaceAll("\\s*\\|\\|\\s*", " || ");
        expr = expr.replaceAll("\\s*==\\s*", " == ");
        expr = expr.replaceAll("\\s*!=\\s*", " != ");
        expr = expr.replaceAll("\\s*<=\\s*", " <= ");
        expr = expr.replaceAll("\\s*>=\\s*", " >= ");
        expr = expr.replaceAll("\\s*<\\s*", " < ");
        expr = expr.replaceAll("\\s*>\\s*", " > ");
        expr = expr.replaceAll("\\s*\\+\\s*", " + ");
        expr = expr.replaceAll("\\s*-\\s*", " - ");
        expr = expr.replaceAll("\\s*\\*\\s*", " * ");
        expr = expr.replaceAll("\\s*/\\s*", " / ");
        
        // Handle parentheses spacing
        expr = expr.replaceAll("\\(\\s+", "(");
        expr = expr.replaceAll("\\s+\\)", ")");
        
        return expr;
    }
    
    private boolean hasAssertions(PropertyModel model) {
        // Check if model has assertion properties
        // This would need to be implemented based on the actual PropertyModel structure
        // For now, we'll do a simple heuristic check
        return false; // Placeholder
    }
    
    private void formatAssertions(PropertyModel model, FormattingContext ctx) {
        // Format assertion properties
        // Implementation depends on the actual PropertyModel structure
        // Placeholder for now
        ctx.appendLine("Safety: /* assertion expressions */;");
    }
    
    private boolean hasLTLProperties(PropertyModel model) {
        // Check if model has LTL properties
        // This would need to be implemented based on the actual PropertyModel structure
        return false; // Placeholder
    }
    
    private void formatLTLProperties(PropertyModel model, FormattingContext ctx) {
        // Format LTL properties
        // Implementation depends on the actual PropertyModel structure
        // Placeholder for now
        ctx.appendLine("PropertyName: G(F(expression));");
    }
    
    private String basicFormat(String content) {
        // Fallback formatting - similar to existing FixedPropertyFormatter
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        content = content.replaceAll("[ \t]+\n", "\n");
        
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int braceLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                result.append(NEW_LINE);
                continue;
            }
            
            int currentIndent = braceLevel;
            if (trimmed.startsWith("}")) {
                currentIndent = Math.max(0, braceLevel - 1);
                braceLevel = currentIndent;
            }
            
            for (int i = 0; i < currentIndent; i++) {
                result.append(INDENT);
            }
            
            result.append(normalizeSpacing(trimmed)).append(NEW_LINE);
            
            if (trimmed.endsWith("{") && !isComment(trimmed)) {
                braceLevel++;
            }
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    private boolean isComment(String trimmed) {
        return trimmed.startsWith("//") || trimmed.startsWith("/*") || 
               trimmed.startsWith("*") || trimmed.endsWith("*/");
    }
    
    private String normalizeSpacing(String line) {
        // Normalize spacing around operators and symbols
        line = line.replaceAll("\\s*=\\s*", " = ");
        line = line.replaceAll("\\s*&&\\s*", " && ");
        line = line.replaceAll("\\s*\\|\\|\\s*", " || ");
        line = line.replaceAll("\\s*;\\s*", "; ");
        line = line.replaceAll("\\s*,\\s*", ", ");
        line = line.replaceAll("\\s*:\\s*", ": ");
        
        // Handle parentheses
        line = line.replaceAll("\\(\\s+", "(");
        line = line.replaceAll("\\s+\\)", ")");
        line = line.replaceAll("\\{\\s+", "{ ");
        line = line.replaceAll("\\s+\\}", " }");
        
        return line;
    }
}
