package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
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
import org.rebecalang.compiler.modelcompiler.RebecaModelCompiler;
import org.rebecalang.compiler.modelcompiler.SymbolTable;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.BlockStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ConstructorDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FieldDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MethodDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MsgsrvDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReactiveClassDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaCode;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Statement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.SynchMethodDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableDeclarator;
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
 * AST-based formatter for Rebeca (.rebeca) files
 */
public class ASTRebecaFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
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
    
    public ASTRebecaFormatter() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("Failed to initialize ASTRebecaFormatter: " + e.getMessage());
        }
    }
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            System.err.println("AST formatting failed: " + e.getMessage());
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
            System.err.println("AST region formatting failed: " + e.getMessage());
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
            
            // Compile the Rebeca file to get AST
            RebecaModel rebecaModel = compileToAST(content);
            if (rebecaModel == null) {
                System.err.println("Failed to compile Rebeca code for formatting");
                return content; // Return original if compilation fails
            }
            
            // Format using AST
            StringBuilder result = new StringBuilder();
            formatRebecaModel(rebecaModel, result, 0);
            
            // Integrate comments back
            String formattedCode = integrateComments(result.toString());
            
            return formattedCode;
            
        } catch (Exception e) {
            System.err.println("Error in AST formatting: " + e.getMessage());
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
                // Make sure it's not inside a string literal
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
    
    private RebecaModel compileToAST(String content) {
        try {
            File tempRebecaFile = File.createTempFile("ASTFormatter", "model.rebeca");
            try {
                FileWriter fstream = new FileWriter(tempRebecaFile);
                BufferedWriter tempRebecaFileWriter = new BufferedWriter(fstream);
                tempRebecaFileWriter.write(content);
                tempRebecaFileWriter.close();

                IProject project = CompilationAndCodeGenerationProcess.getProject();
                if (project == null) {
                    // Use default settings if no project context
                    Set<CompilerExtension> extensions = Set.of();
                    CoreVersion version = CoreVersion.CORE_2_3;
                    
                    Pair<RebecaModel, SymbolTable> compilationResult = 
                            modelCompiler.compileRebecaFile(tempRebecaFile, extensions, version);
                    
                    return compilationResult != null ? compilationResult.getFirst() : null;
                }
                
                Set<CompilerExtension> compilerExtensions = 
                        CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
                
                CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
                
                Pair<RebecaModel, SymbolTable> compilationResult = 
                        modelCompiler.compileRebecaFile(tempRebecaFile, compilerExtensions, version);
                
                return compilationResult != null ? compilationResult.getFirst() : null;
                
            } finally {
                if (tempRebecaFile.exists()) {
                    tempRebecaFile.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to compile Rebeca code: " + e.getMessage());
            return null;
        }
    }
    
    private void formatRebecaModel(RebecaModel model, StringBuilder sb, int indentLevel) {
        // Rebeca language doesn't typically use package declarations or imports
        // so we skip formatting those and go directly to the code
        
        if (model.getRebecaCode() != null) {
            formatRebecaCode(model.getRebecaCode(), sb, indentLevel);
        }
    }
    
    private void formatRebecaCode(RebecaCode code, StringBuilder sb, int indentLevel) {
        // Format reactive class declarations
        if (code.getReactiveClassDeclaration() != null) {
            for (ReactiveClassDeclaration rcd : code.getReactiveClassDeclaration()) {
                formatReactiveClassDeclaration(rcd, sb, indentLevel);
                sb.append(NEW_LINE);
            }
        }
        
        // Format main declaration
        if (code.getMainDeclaration() != null) {
            formatMainDeclaration(code.getMainDeclaration(), sb, indentLevel);
        }
    }
    
    private void formatReactiveClassDeclaration(ReactiveClassDeclaration rcd, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        sb.append("reactiveclass ").append(rcd.getName());
        if (rcd.getQueueSize() != null) {
            sb.append("(").append(rcd.getQueueSize()).append(")");
        }
        sb.append(" {").append(NEW_LINE);
        
        // Format knownrebecs
        if (rcd.getKnownRebecs() != null && !rcd.getKnownRebecs().isEmpty()) {
            appendIndent(sb, indentLevel + 1);
            sb.append("knownrebecs {").append(NEW_LINE);
            for (FieldDeclaration field : rcd.getKnownRebecs()) {
                formatFieldDeclaration(field, sb, indentLevel + 2);
            }
            appendIndent(sb, indentLevel + 1);
            sb.append("}").append(NEW_LINE);
        }
        
        // Format statevars
        if (rcd.getStatevars() != null && !rcd.getStatevars().isEmpty()) {
            appendIndent(sb, indentLevel + 1);
            sb.append("statevars {").append(NEW_LINE);
            for (FieldDeclaration field : rcd.getStatevars()) {
                formatFieldDeclaration(field, sb, indentLevel + 2);
            }
            appendIndent(sb, indentLevel + 1);
            sb.append("}").append(NEW_LINE);
        }
        
        // Format constructors
        if (rcd.getConstructors() != null && !rcd.getConstructors().isEmpty()) {
            for (ConstructorDeclaration constructor : rcd.getConstructors()) {
                formatConstructorDeclaration(constructor, sb, indentLevel + 1);
            }
        }
        
        // Format msgsrvs
        if (rcd.getMsgsrvs() != null && !rcd.getMsgsrvs().isEmpty()) {
            for (MsgsrvDeclaration msgsrv : rcd.getMsgsrvs()) {
                formatMsgsrvDeclaration(msgsrv, sb, indentLevel + 1);
            }
        }
        
        // Format synch methods
        if (rcd.getSynchMethods() != null && !rcd.getSynchMethods().isEmpty()) {
            for (SynchMethodDeclaration synchMethod : rcd.getSynchMethods()) {
                formatSynchMethodDeclaration(synchMethod, sb, indentLevel + 1);
            }
        }
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatFieldDeclaration(FieldDeclaration field, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        
        if (field.getType() != null) {
            sb.append(field.getType().getTypeName());
        }
        
        if (field.getVariableDeclarators() != null && !field.getVariableDeclarators().isEmpty()) {
            sb.append(" ");
            for (int i = 0; i < field.getVariableDeclarators().size(); i++) {
                if (i > 0) sb.append(", ");
                VariableDeclarator var = field.getVariableDeclarators().get(i);
                sb.append(var.getVariableName());
                if (var.getVariableInitializer() != null) {
                    sb.append(" = ");
                    // Format variable initializer - could be improved to handle expressions
                    sb.append("/* initializer */");
                }
            }
        }
        
        sb.append(";").append(NEW_LINE);
    }
    
    private void formatConstructorDeclaration(ConstructorDeclaration constructor, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        sb.append(constructor.getName()).append("(");
        
        if (constructor.getFormalParameters() != null && !constructor.getFormalParameters().isEmpty()) {
            for (int i = 0; i < constructor.getFormalParameters().size(); i++) {
                if (i > 0) sb.append(", ");
                var param = constructor.getFormalParameters().get(i);
                if (param.getType() != null) {
                    sb.append(param.getType().getTypeName()).append(" ");
                }
                sb.append(param.getName());
            }
        }
        
        sb.append(") {").append(NEW_LINE);
        
        if (constructor.getBlock() != null) {
            formatBlockStatement(constructor.getBlock(), sb, indentLevel + 1);
        }
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatMsgsrvDeclaration(MsgsrvDeclaration msgsrv, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        sb.append("msgsrv ").append(msgsrv.getName()).append("(");
        
        if (msgsrv.getFormalParameters() != null && !msgsrv.getFormalParameters().isEmpty()) {
            for (int i = 0; i < msgsrv.getFormalParameters().size(); i++) {
                if (i > 0) sb.append(", ");
                var param = msgsrv.getFormalParameters().get(i);
                if (param.getType() != null) {
                    sb.append(param.getType().getTypeName()).append(" ");
                }
                sb.append(param.getName());
            }
        }
        
        sb.append(") {").append(NEW_LINE);
        
        if (msgsrv.getBlock() != null) {
            formatBlockStatement(msgsrv.getBlock(), sb, indentLevel + 1);
        }
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatSynchMethodDeclaration(SynchMethodDeclaration synchMethod, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        
        if (synchMethod.getReturnType() != null) {
            sb.append(synchMethod.getReturnType().getTypeName()).append(" ");
        }
        
        sb.append(synchMethod.getName()).append("(");
        
        if (synchMethod.getFormalParameters() != null && !synchMethod.getFormalParameters().isEmpty()) {
            for (int i = 0; i < synchMethod.getFormalParameters().size(); i++) {
                if (i > 0) sb.append(", ");
                var param = synchMethod.getFormalParameters().get(i);
                if (param.getType() != null) {
                    sb.append(param.getType().getTypeName()).append(" ");
                }
                sb.append(param.getName());
            }
        }
        
        sb.append(") {").append(NEW_LINE);
        
        if (synchMethod.getBlock() != null) {
            formatBlockStatement(synchMethod.getBlock(), sb, indentLevel + 1);
        }
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatBlockStatement(BlockStatement block, StringBuilder sb, int indentLevel) {
        if (block.getStatements() != null) {
            for (Statement stmt : block.getStatements()) {
                formatStatement(stmt, sb, indentLevel);
            }
        }
    }
    
    private void formatStatement(Statement stmt, StringBuilder sb, int indentLevel) {
        // Simplified statement formatting - can be expanded
        //appendIndent(sb, indentLevel);
        //sb.append("/* statement */;").append(NEW_LINE);
        
        if (stmt == null) {
            return;
        }
        
        appendIndent(sb, indentLevel);
        
        // Format based on statement type
        String className = stmt.getClass().getSimpleName();
        
        switch (className) {
            case "ConditionalStatement":
                sb.append("if (/* condition */) {").append(NEW_LINE);
                // Could format nested statements here
                appendIndent(sb, indentLevel);
                sb.append("}").append(NEW_LINE);
                break;
                
            case "WhileStatement":
                sb.append("while (/* condition */) {").append(NEW_LINE);
                // Could format nested statements here
                appendIndent(sb, indentLevel);
                sb.append("}").append(NEW_LINE);
                break;
                
            case "ForStatement":
                sb.append("for (/* init */; /* condition */; /* update */) {").append(NEW_LINE);
                // Could format nested statements here
                appendIndent(sb, indentLevel);
                sb.append("}").append(NEW_LINE);
                break;
                
            case "ReturnStatement":
                sb.append("return /* value */;").append(NEW_LINE);
                break;
                
            case "BreakStatement":
                sb.append("break;").append(NEW_LINE);
                break;
                
            case "ContinueStatement":
                sb.append("continue;").append(NEW_LINE);
                break;
                
            case "BlockStatement":
                // Block statements are handled separately
                formatBlockStatement((BlockStatement) stmt, sb, indentLevel);
                return; // Don't add extra newline
                
            default:
                // For other statement types, use a generic placeholder
                sb.append("/* statement: ").append(className).append(" */;").append(NEW_LINE);
                break;
        }
    }
    
    private void formatMainDeclaration(MainDeclaration main, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        sb.append("main {").append(NEW_LINE);
        
        if (main.getMainRebecDefinition() != null) {
            for (MainRebecDefinition mrd : main.getMainRebecDefinition()) {
                formatMainRebecDefinition(mrd, sb, indentLevel + 1);
            }
        }
        
        appendIndent(sb, indentLevel);
        sb.append("}").append(NEW_LINE);
    }
    
    private void formatMainRebecDefinition(MainRebecDefinition mrd, StringBuilder sb, int indentLevel) {
        appendIndent(sb, indentLevel);
        
        if (mrd.getType() != null) {
            sb.append(mrd.getType().getTypeName()).append(" ");
        }
        
        sb.append(mrd.getName());
        
        if (mrd.getBindings() != null && !mrd.getBindings().isEmpty()) {
            sb.append("(");
            for (int i = 0; i < mrd.getBindings().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(mrd.getBindings().get(i).toString());
            }
            sb.append(")");
        }
        
        sb.append(":(");
        
        if (mrd.getArguments() != null && !mrd.getArguments().isEmpty()) {
            for (int i = 0; i < mrd.getArguments().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(mrd.getArguments().get(i).toString());
            }
        }
        
        sb.append(");").append(NEW_LINE);
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
        // For now, just append comments at appropriate lines
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
