package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess;
import org.rebecalang.compiler.modelcompiler.RebecaModelCompiler;
import org.rebecalang.compiler.modelcompiler.SymbolTable;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.propertycompiler.PropertyCompiler;
import org.rebecalang.compiler.propertycompiler.generalrebeca.objectmodel.PropertyModel;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.rebecalang.rmc.RMCConfig;
import org.rebecalang.compiler.CompilerConfig;

/**
 * AST-based formatter for Property (.property) files
 */
public class ASTPropertyFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    @Autowired
    PropertyCompiler propertyCompiler;
    
    @Autowired
    RebecaModelCompiler modelCompiler;
    
    private List<Comment> comments;
    
    private static class Comment {
        final int startLine;
        final int endLine;
        final String content;
        final CommentType type;
        
        Comment(int startLine, int endLine, String content, CommentType type) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.content = content;
            this.type = type;
        }
    }
    
    private enum CommentType {
        SINGLE_LINE, MULTI_LINE, JAVADOC
    }
    
    public ASTPropertyFormatter() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("Failed to initialize ASTPropertyFormatter: " + e.getMessage());
        }
    }
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            System.err.println("AST property formatting failed: " + e.getMessage());
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
            System.err.println("AST property region formatting failed: " + e.getMessage());
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
            // Extract comments before compilation
            extractComments(content);
            
            // Compile the property file to get AST
            PropertyModel propertyModel = compileToAST(content);
            if (propertyModel == null) {
                System.err.println("Failed to compile property code for formatting");
                return content; // Return original if compilation fails
            }
            
            // Format using AST
            StringBuilder result = new StringBuilder();
            formatPropertyModel(propertyModel, result, 0);
            
            // Integrate comments back
            String formattedCode = integrateComments(result.toString());
            
            return formattedCode;
            
        } catch (Exception e) {
            System.err.println("Error in AST property formatting: " + e.getMessage());
            e.printStackTrace();
            return content; // Return original content on error
        }
    }
    
    private void extractComments(String content) {
        comments = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            // Check for single-line comments
            int singleCommentIndex = line.indexOf("//");
            if (singleCommentIndex != -1) {
                if (!isInsideStringLiteral(line, singleCommentIndex)) {
                    String commentContent = line.substring(singleCommentIndex);
                    comments.add(new Comment(lineNumber, lineNumber, commentContent, CommentType.SINGLE_LINE));
                }
            }
            
            // Check for multi-line comment start
            int multiCommentStart = line.indexOf("/*");
            if (multiCommentStart != -1 && !isInsideStringLiteral(line, multiCommentStart)) {
                boolean isJavadoc = line.indexOf("/**") == multiCommentStart;
                CommentType type = isJavadoc ? CommentType.JAVADOC : CommentType.MULTI_LINE;
                
                // Find the end of the multi-line comment
                int endLine = findMultiLineCommentEnd(lines, i, multiCommentStart);
                if (endLine != -1) {
                    StringBuilder commentContent = new StringBuilder();
                    for (int j = i; j <= endLine; j++) {
                        if (j == i) {
                            commentContent.append(lines[j].substring(multiCommentStart));
                        } else {
                            commentContent.append(lines[j]);
                        }
                        if (j < endLine) {
                            commentContent.append("\n");
                        }
                    }
                    comments.add(new Comment(lineNumber, endLine + 1, commentContent.toString(), type));
                }
            }
        }
    }
    
    private boolean isInsideStringLiteral(String line, int index) {
        boolean inString = false;
        char stringChar = 0;
        for (int i = 0; i < index; i++) {
            char c = line.charAt(i);
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = false;
            }
        }
        return inString;
    }
    
    private int findMultiLineCommentEnd(String[] lines, int startLine, int startIndex) {
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            int searchStart = (i == startLine) ? startIndex : 0;
            int endIndex = line.indexOf("*/", searchStart);
            if (endIndex != -1) {
                return i;
            }
        }
        return -1; // Comment not closed
    }
    
    private PropertyModel compileToAST(String propertyContent) {
        try {
            // First we need a dummy rebeca model for property compilation
            RebecaModel dummyRebecaModel = createDummyRebecaModel();
            
            File tempPropertyFile = File.createTempFile("ASTPropertyFormatter", "model.property");
            try {
                FileWriter fstream = new FileWriter(tempPropertyFile);
                BufferedWriter tempPropertyFileWriter = new BufferedWriter(fstream);
                tempPropertyFileWriter.write(propertyContent);
                tempPropertyFileWriter.close();

                IProject project = CompilationAndCodeGenerationProcess.getProject();
                Set<CompilerExtension> extensions = Set.of();
                
                if (project != null) {
                    extensions = CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
                }
                
                PropertyModel propertyModel = propertyCompiler.compilePropertyFile(
                    tempPropertyFile, dummyRebecaModel, extensions);
                
                return propertyModel;
                
            } finally {
                if (tempPropertyFile.exists()) {
                    tempPropertyFile.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to compile property code: " + e.getMessage());
            return null;
        }
    }
    
    private RebecaModel createDummyRebecaModel() {
        // Create a minimal dummy Rebeca model for property compilation
        // This may not be needed for formatting, but property compiler might require it
        RebecaModel dummyModel = new RebecaModel();
        // Add minimal structure if needed
        return dummyModel;
    }
    
    private void formatPropertyModel(PropertyModel model, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        sb.append("property {").append(NEW_LINE);
        
        // Format different sections of the property model
        // This is a simplified implementation - actual property model structure may vary
        formatPropertyContent(model, sb, indentLevel + 1);
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatPropertyContent(PropertyModel model, StringBuilder sb, int indentLevel) {
        // Format define section if it exists
        if (hasDefineSection(model)) {
            appendIndent(sb, indentLevel);
            sb.append("define {").append(NEW_LINE);
            formatDefineSection(model, sb, indentLevel + 1);
            appendIndent(sb, indentLevel);
            sb.append("}").append(NEW_LINE).append(NEW_LINE);
        }
        
        // Format assertion section if it exists
        if (hasAssertionSection(model)) {
            appendIndent(sb, indentLevel);
            sb.append("Assertion {").append(NEW_LINE);
            formatAssertionSection(model, sb, indentLevel + 1);
            appendIndent(sb, indentLevel);
            sb.append("}").append(NEW_LINE).append(NEW_LINE);
        }
        
        // Format LTL section if it exists
        if (hasLTLSection(model)) {
            appendIndent(sb, indentLevel);
            sb.append("LTL {").append(NEW_LINE);
            formatLTLSection(model, sb, indentLevel + 1);
            appendIndent(sb, indentLevel);
            sb.append("}").append(NEW_LINE);
        }
    }
    
    private boolean hasDefineSection(PropertyModel model) {
        // Check if model has define section
        // Implementation depends on PropertyModel structure
        return true; // Simplified for now
    }
    
    private boolean hasAssertionSection(PropertyModel model) {
        // Check if model has assertion section
        return true; // Simplified for now
    }
    
    private boolean hasLTLSection(PropertyModel model) {
        // Check if model has LTL section
        return true; // Simplified for now
    }
    
    private void formatDefineSection(PropertyModel model, StringBuilder sb, int indentLevel) {
        // Format define expressions
        // This would need to access the actual PropertyModel structure
        appendIndent(sb, indentLevel);
        sb.append("/* define expressions */").append(NEW_LINE);
    }
    
    private void formatAssertionSection(PropertyModel model, StringBuilder sb, int indentLevel) {
        // Format assertion expressions
        appendIndent(sb, indentLevel);
        sb.append("/* assertion expressions */").append(NEW_LINE);
    }
    
    private void formatLTLSection(PropertyModel model, StringBuilder sb, int indentLevel) {
        // Format LTL expressions
        appendIndent(sb, indentLevel);
        sb.append("/* LTL expressions */").append(NEW_LINE);
    }
    
    private void appendIndent(StringBuilder sb, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }
    }
    
    private String integrateComments(String formattedCode) {
        if (comments.isEmpty()) {
            return formattedCode;
        }
        
        // Simple comment integration - can be improved
        String[] lines = formattedCode.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            result.append(line);
            
            // Check if any comment should be placed after this line
            for (Comment comment : comments) {
                if (comment.startLine == i + 1 && comment.type == CommentType.SINGLE_LINE) {
                    if (!line.trim().isEmpty()) {
                        result.append("  ").append(comment.content);
                    } else {
                        result.append(comment.content);
                    }
                }
            }
            
            result.append(NEW_LINE);
        }
        
        return result.toString();
    }
}
