package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FieldDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MethodDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReactiveClassDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableDeclarator;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.rmc.RMCConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * AST-based formatter for Rebeca (.rebeca) files that uses the compiler's AST
 * to properly format code while preserving comments and maintaining proper indentation.
 */
public class ASTBasedRebecaFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    @Autowired
    private RebecaModelCompiler modelCompiler;
    
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
        MULTI_LINE,     // /* */
        JAVADOC         // /** */
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
                case JAVADOC:
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
    
    public ASTBasedRebecaFormatter() {
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("ASTBasedRebecaFormatter: Failed to initialize compiler components: " + e.getMessage());
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
            RebecaModel astModel = parseAST(cleanContent);
            
            // Step 4: Format using AST + preserved comments
            if (astModel != null && astModel.getRebecaCode() != null) {
                return formatWithAST(astModel, comments);
            } else {
                // Fallback to basic formatting if AST parsing fails
                return basicFormat(content);
            }
            
        } catch (Exception e) {
            System.err.println("AST formatting failed, using fallback: " + e.getMessage());
            return basicFormat(content);
        }
    }
    
    private List<Comment> extractComments(String content) {
        List<Comment> comments = new ArrayList<>();
        
        // Patterns for different comment types
        Pattern singleLinePattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        Pattern multiLinePattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        Pattern javadocPattern = Pattern.compile("/\\*\\*.*?\\*/", Pattern.DOTALL);
        
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
        
        // Check for multi-line comments (including javadoc)
        Matcher javadocMatcher = javadocPattern.matcher(content);
        while (javadocMatcher.find()) {
            int line = getLineNumber(content, javadocMatcher.start());
            int column = getColumnNumber(content, javadocMatcher.start());
            String commentContent = javadocMatcher.group();
            comments.add(new Comment(line, column, commentContent, CommentType.JAVADOC));
        }
        
        Matcher multiLineMatcher = multiLinePattern.matcher(content);
        while (multiLineMatcher.find()) {
            // Skip if it's already matched as javadoc
            if (!javadocMatcher.find(multiLineMatcher.start())) {
                int line = getLineNumber(content, multiLineMatcher.start());
                int column = getColumnNumber(content, multiLineMatcher.start());
                String commentContent = multiLineMatcher.group();
                comments.add(new Comment(line, column, commentContent, CommentType.MULTI_LINE));
            }
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
    
    private RebecaModel parseAST(String content) {
        try {
            // Create temporary file for parsing
            File tempFile = File.createTempFile("rebeca_format_", ".rebeca");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }
            
            // Parse using the existing compiler infrastructure
            Set<CompilerExtension> extensions = new HashSet<>();
            CoreVersion version = CoreVersion.CORE_2_1; // Default version
            
            if (modelCompiler != null) {
                Pair<RebecaModel, ?> result = modelCompiler.compileRebecaFile(tempFile, extensions, version);
                tempFile.delete();
                return result.getFirst();
            }
            
            tempFile.delete();
            return null;
            
        } catch (Exception e) {
            System.err.println("Failed to parse AST: " + e.getMessage());
            return null;
        }
    }
    
    private String formatWithAST(RebecaModel model, List<Comment> comments) {
        FormattingContext ctx = new FormattingContext();
        ctx.comments = comments;
        
        // Format reactive class declarations
        if (model.getRebecaCode().getReactiveClassDeclaration() != null) {
            for (ReactiveClassDeclaration classDecl : model.getRebecaCode().getReactiveClassDeclaration()) {
                ctx.appendCommentsBefore(classDecl.getLineNumber());
                formatReactiveClass(classDecl, ctx);
                ctx.appendEmptyLine();
            }
        }
        
        // Format main section
        if (model.getRebecaCode().getMainDeclaration() != null) {
            ctx.appendCommentsBefore(model.getRebecaCode().getMainDeclaration().getLineNumber());
            formatMainDeclaration(model.getRebecaCode().getMainDeclaration(), ctx);
        }
        
        return ctx.getResult();
    }
    
    private void formatReactiveClass(ReactiveClassDeclaration classDecl, FormattingContext ctx) {
        // Class declaration line
        StringBuilder classLine = new StringBuilder();
        classLine.append("reactiveclass ").append(classDecl.getName());
        
        if (classDecl.getQueueLength() != null) {
            classLine.append("(").append(classDecl.getQueueLength()).append(")");
        }
        
        if (classDecl.getExtends() != null && !classDecl.getExtends().isEmpty()) {
            classLine.append(" extends ");
            for (int i = 0; i < classDecl.getExtends().size(); i++) {
                if (i > 0) classLine.append(", ");
                classLine.append(classDecl.getExtends().get(i).getTypeName());
            }
        }
        
        classLine.append(" {");
        ctx.appendLine(classLine.toString());
        ctx.increaseIndent();
        
        // Known rebecs section
        if (classDecl.getKnownRebecs() != null && !classDecl.getKnownRebecs().isEmpty()) {
            ctx.appendLine("knownrebecs {");
            ctx.increaseIndent();
            
            for (FieldDeclaration field : classDecl.getKnownRebecs()) {
                StringBuilder fieldLine = new StringBuilder();
                fieldLine.append(field.getType().getTypeName()).append(" ");
                
                for (int i = 0; i < field.getVariableDeclarators().size(); i++) {
                    if (i > 0) fieldLine.append(", ");
                    fieldLine.append(field.getVariableDeclarators().get(i).getVariableName());
                }
                fieldLine.append(";");
                ctx.appendLine(fieldLine.toString());
            }
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        // State variables section
        if (classDecl.getStatevars() != null && !classDecl.getStatevars().isEmpty()) {
            ctx.appendLine("statevars {");
            ctx.increaseIndent();
            
            for (FieldDeclaration field : classDecl.getStatevars()) {
                StringBuilder fieldLine = new StringBuilder();
                fieldLine.append(field.getType().getTypeName()).append(" ");
                
                for (int i = 0; i < field.getVariableDeclarators().size(); i++) {
                    if (i > 0) fieldLine.append(", ");
                    VariableDeclarator var = field.getVariableDeclarators().get(i);
                    fieldLine.append(var.getVariableName());
                    if (var.getVariableInitializer() != null) {
                        fieldLine.append(" = ").append(var.getVariableInitializer().toString());
                    }
                }
                fieldLine.append(";");
                ctx.appendLine(fieldLine.toString());
            }
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        // Constructor and methods
        if (classDecl.getConstructors() != null) {
            for (MethodDeclaration constructor : classDecl.getConstructors()) {
                ctx.appendEmptyLine();
                formatMethod(constructor, ctx, true);
            }
        }
        
        if (classDecl.getMsgsrvs() != null) {
            for (MethodDeclaration method : classDecl.getMsgsrvs()) {
                ctx.appendEmptyLine();
                formatMethod(method, ctx, false);
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    private void formatMethod(MethodDeclaration method, FormattingContext ctx, boolean isConstructor) {
        StringBuilder methodLine = new StringBuilder();
        
        if (isConstructor) {
            methodLine.append(method.getName());
        } else {
            methodLine.append("msgsrv ").append(method.getName());
        }
        
        methodLine.append("(");
        if (method.getFormalParameters() != null) {
            for (int i = 0; i < method.getFormalParameters().size(); i++) {
                if (i > 0) methodLine.append(", ");
                FieldDeclaration param = method.getFormalParameters().get(i);
                methodLine.append(param.getType().getTypeName()).append(" ");
                methodLine.append(param.getVariableDeclarators().get(0).getVariableName());
            }
        }
        methodLine.append(") {");
        
        ctx.appendLine(methodLine.toString());
        ctx.increaseIndent();
        
        // Method body - simplified for now, would need more sophisticated AST traversal
        // for complete statement formatting
        if (method.getBlock() != null && method.getBlock().getStatements() != null) {
            for (Object stmt : method.getBlock().getStatements()) {
                ctx.appendLine(stmt.toString() + ";");
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    private void formatMainDeclaration(org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainDeclaration mainDecl, FormattingContext ctx) {
        ctx.appendLine("main {");
        ctx.increaseIndent();
        
        if (mainDecl.getMainRebecDefinition() != null) {
            for (MainRebecDefinition def : mainDecl.getMainRebecDefinition()) {
                StringBuilder defLine = new StringBuilder();
                defLine.append(def.getType().getTypeName()).append(" ");
                defLine.append(def.getName());
                
                if (def.getBindings() != null && !def.getBindings().isEmpty()) {
                    defLine.append("(");
                    for (int i = 0; i < def.getBindings().size(); i++) {
                        if (i > 0) defLine.append(", ");
                        defLine.append(def.getBindings().get(i).toString());
                    }
                    defLine.append(")");
                }
                
                defLine.append(":(");
                if (def.getArguments() != null && !def.getArguments().isEmpty()) {
                    for (int i = 0; i < def.getArguments().size(); i++) {
                        if (i > 0) defLine.append(", ");
                        defLine.append(def.getArguments().get(i).toString());
                    }
                }
                defLine.append(");");
                
                ctx.appendLine(defLine.toString());
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    private String basicFormat(String content) {
        // Fallback formatting - similar to existing FixedRebecaFormatter
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
            
            result.append(trimmed).append(NEW_LINE);
            
            if (trimmed.endsWith("{")) {
                braceLevel++;
            }
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
}
