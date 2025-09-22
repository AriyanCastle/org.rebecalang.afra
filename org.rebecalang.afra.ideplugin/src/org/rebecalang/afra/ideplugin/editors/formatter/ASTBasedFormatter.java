package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.modelcompiler.RebecaModelCompiler;
import org.rebecalang.compiler.modelcompiler.SymbolTable;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.rmc.RMCConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * AST-based formatter for Rebeca files that properly handles:
 * - Complete AST-based formatting
 * - Comment preservation and formatting
 * - Proper indentation
 * - Normalized spacing
 * - Both .rebeca and .property files
 */
public class ASTBasedFormatter implements IAfraFormatter {
    
    @Autowired
    private RebecaModelCompiler modelCompiler;
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    // Comment patterns
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//(.*)");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*([\\s\\S]*?)\\*/");
    private static final Pattern DOC_COMMENT = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/");
    
    /**
     * Represents a comment found in the source code
     */
    public static class Comment {
        public enum Type { SINGLE_LINE, MULTI_LINE, DOC_COMMENT }
        
        public final Type type;
        public final String content;
        public final int startOffset;
        public final int endOffset;
        public final int line;
        public final int column;
        
        public Comment(Type type, String content, int startOffset, int endOffset, int line, int column) {
            this.type = type;
            this.content = content;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.line = line;
            this.column = column;
        }
        
        @Override
        public String toString() {
            switch (type) {
                case SINGLE_LINE:
                    return "//" + content;
                case DOC_COMMENT:
                    return "/**" + content + "*/";
                case MULTI_LINE:
                default:
                    return "/*" + content + "*/";
            }
        }
    }
    
    /**
     * Context for formatting operations
     */
    public static class FormattingContext {
        public int indentLevel = 0;
        public boolean isInMainSection = false;
        public boolean isInClass = false;
        public boolean isInMethod = false;
        public boolean needsBlankLine = false;
        public StringBuilder output = new StringBuilder();
        public List<Comment> comments = new ArrayList<>();
        public String originalContent = "";
        
        public void indent() {
            for (int i = 0; i < indentLevel; i++) {
                output.append(INDENT);
            }
        }
        
        public void newLine() {
            output.append(NEW_LINE);
        }
        
        public void increaseIndent() {
            indentLevel++;
        }
        
        public void decreaseIndent() {
            if (indentLevel > 0) {
                indentLevel--;
            }
        }
        
        public void append(String text) {
            output.append(text);
        }
        
        public void appendLine(String text) {
            indent();
            append(text);
            newLine();
        }
        
        public void blankLine() {
            if (output.length() > 0 && !output.toString().endsWith(NEW_LINE + NEW_LINE)) {
                newLine();
            }
        }
    }
    
    public ASTBasedFormatter() {
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("Failed to initialize compiler components: " + e.getMessage());
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
     * Main formatting method that determines file type and applies appropriate formatting
     */
    private String formatContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Determine if this is a .property file or .rebeca file
        if (isPropertyFile(content)) {
            return formatPropertyContent(content);
        } else {
            return formatRebecaContent(content);
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
     * Format Rebeca source files using AST
     */
    private String formatRebecaContent(String content) {
        try {
            FormattingContext context = new FormattingContext();
            context.originalContent = content;
            
            // Extract comments before parsing
            extractComments(content, context);
            
            // Parse with AST
            RebecaModel model = parseRebecaFile(content);
            if (model != null) {
                // Use AST-based formatting
                formatWithAST(model, context);
            } else {
                // Fallback to simple formatting if AST parsing fails
                return formatContentSimple(content);
            }
            
            return context.output.toString();
            
        } catch (Exception e) {
            System.err.println("AST formatting failed, falling back to simple formatting: " + e.getMessage());
            return formatContentSimple(content);
        }
    }
    
    /**
     * Parse Rebeca file to get AST
     */
    private RebecaModel parseRebecaFile(String content) {
        try {
            if (modelCompiler == null) {
                System.err.println("Model compiler not initialized");
                return null;
            }
            
            // Create temporary file
            File tempFile = File.createTempFile("formatter", ".rebeca");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }
            
            // Get compilation extensions (assuming core Rebeca for now)
            Set<CompilerExtension> extensions = Collections.emptySet();
            CoreVersion version = CoreVersion.CORE_2_1;
            
            // Compile to get AST
            Pair<RebecaModel, SymbolTable> result = modelCompiler.compileRebecaFile(tempFile, extensions, version);
            
            // Clean up
            tempFile.delete();
            
            return result != null ? result.getFirst() : null;
            
        } catch (Exception e) {
            System.err.println("Failed to parse Rebeca file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract comments from source content
     */
    private void extractComments(String content, FormattingContext context) {
        List<Comment> comments = new ArrayList<>();
        
        // Extract doc comments (/** ... */)
        Matcher docMatcher = DOC_COMMENT.matcher(content);
        while (docMatcher.find()) {
            int line = getLineNumber(content, docMatcher.start());
            int column = getColumnNumber(content, docMatcher.start());
            comments.add(new Comment(Comment.Type.DOC_COMMENT, docMatcher.group(1), 
                                   docMatcher.start(), docMatcher.end(), line, column));
        }
        
        // Extract multi-line comments (/* ... */) - excluding doc comments
        String withoutDocComments = DOC_COMMENT.matcher(content).replaceAll("");
        Matcher multiMatcher = MULTI_LINE_COMMENT.matcher(withoutDocComments);
        while (multiMatcher.find()) {
            int line = getLineNumber(content, multiMatcher.start());
            int column = getColumnNumber(content, multiMatcher.start());
            comments.add(new Comment(Comment.Type.MULTI_LINE, multiMatcher.group(1), 
                                   multiMatcher.start(), multiMatcher.end(), line, column));
        }
        
        // Extract single-line comments (//)
        String withoutMultiComments = MULTI_LINE_COMMENT.matcher(withoutDocComments).replaceAll("");
        Matcher singleMatcher = SINGLE_LINE_COMMENT.matcher(withoutMultiComments);
        while (singleMatcher.find()) {
            int line = getLineNumber(content, singleMatcher.start());
            int column = getColumnNumber(content, singleMatcher.start());
            comments.add(new Comment(Comment.Type.SINGLE_LINE, singleMatcher.group(1), 
                                   singleMatcher.start(), singleMatcher.end(), line, column));
        }
        
        // Sort comments by position
        comments.sort(Comparator.comparingInt(c -> c.startOffset));
        context.comments = comments;
    }
    
    /**
     * Get line number for a given offset
     */
    private int getLineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    /**
     * Get column number for a given offset
     */
    private int getColumnNumber(String content, int offset) {
        int column = 1;
        for (int i = offset - 1; i >= 0; i--) {
            if (content.charAt(i) == '\n') {
                break;
            }
            column++;
        }
        return column;
    }
    
    /**
     * Format using AST with visitor pattern
     */
    private void formatWithAST(RebecaModel model, FormattingContext context) {
        try {
            RebecaASTVisitor visitor = new RebecaASTVisitor(context);
            
            // Add any comments that appear at the beginning of the file
            addLeadingComments(context);
            
            // Visit the AST to generate formatted output
            visitor.visitRebecaModel(model);
            
            // Add any trailing comments
            addTrailingComments(context);
        } catch (Exception e) {
            // If AST processing fails, fall back to simple formatting
            System.err.println("AST visitor failed, using simple formatting: " + e.getMessage());
            throw e; // Re-throw to trigger fallback in calling method
        }
    }
    
    /**
     * Add comments that appear at the beginning of the file
     */
    private void addLeadingComments(FormattingContext context) {
        for (Comment comment : context.comments) {
            if (comment.line <= 5) { // Consider first 5 lines as leading
                formatComment(comment, context);
                context.newLine();
            }
        }
    }
    
    /**
     * Add comments that appear at the end of the file
     */
    private void addTrailingComments(FormattingContext context) {
        // For now, we'll integrate comments inline with the AST visitor
        // This is a placeholder for file-level trailing comments
    }
    
    /**
     * Format a comment with proper indentation and style
     */
    private void formatComment(Comment comment, FormattingContext context) {
        switch (comment.type) {
            case SINGLE_LINE:
                context.indent();
                context.append("//");
                context.append(comment.content.trim());
                break;
                
            case DOC_COMMENT:
                formatMultiLineComment(comment, context, "/**", "*/");
                break;
                
            case MULTI_LINE:
                formatMultiLineComment(comment, context, "/*", "*/");
                break;
        }
    }
    
    /**
     * Format multi-line comments with proper indentation
     */
    private void formatMultiLineComment(Comment comment, FormattingContext context, String start, String end) {
        String content = comment.content.trim();
        String[] lines = content.split("\\r?\\n");
        
        if (lines.length == 1 && content.length() < 50) {
            // Short single-line comment
            context.indent();
            context.append(start);
            context.append(content);
            context.append(end);
        } else {
            // Multi-line comment
            context.indent();
            context.append(start);
            context.newLine();
            
            for (String line : lines) {
                context.indent();
                context.append(" * ");
                context.append(line.trim());
                context.newLine();
            }
            
            context.indent();
            context.append(" ");
            context.append(end);
        }
    }
    
    /**
     * Simple fallback formatting
     */
    private String formatContentSimple(String content) {
        // Use the fallback formatter which doesn't depend on specific API
        FallbackRebecaFormatter fallback = new FallbackRebecaFormatter();
        try {
            // Create a temporary document to use the public interface
            org.eclipse.jface.text.Document doc = new org.eclipse.jface.text.Document(content);
            return fallback.format(doc);
        } catch (Exception e) {
            return content;
        }
    }
    
    /**
     * Format property files
     */
    private String formatPropertyContent(String content) {
        FormattingContext context = new FormattingContext();
        context.originalContent = content;
        
        // Extract comments
        extractComments(content, context);
        
        // Use dedicated property formatter
        PropertyASTFormatter propertyFormatter = new PropertyASTFormatter();
        return propertyFormatter.formatPropertyContent(content, context);
    }
    
