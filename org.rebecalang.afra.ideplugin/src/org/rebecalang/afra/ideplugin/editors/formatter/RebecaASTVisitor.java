package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.List;

import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FieldDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MethodDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReactiveClassDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaCode;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Type;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableDeclarator;

/**
 * AST visitor for formatting Rebeca source code.
 * Implements the visitor pattern to traverse and format AST nodes.
 */
public class RebecaASTVisitor {
    
    private final ASTBasedFormatter.FormattingContext context;
    
    public RebecaASTVisitor(ASTBasedFormatter.FormattingContext context) {
        this.context = context;
    }
    
    /**
     * Format the entire Rebeca model
     */
    public void visitRebecaModel(RebecaModel model) {
        if (model == null || model.getRebecaCode() == null) {
            return;
        }
        
        visitRebecaCode(model.getRebecaCode());
    }
    
    /**
     * Format the main Rebeca code structure
     */
    public void visitRebecaCode(RebecaCode rebecaCode) {
        // Format reactive class declarations
        if (rebecaCode.getReactiveClassDeclaration() != null) {
            for (int i = 0; i < rebecaCode.getReactiveClassDeclaration().size(); i++) {
                ReactiveClassDeclaration rcd = rebecaCode.getReactiveClassDeclaration().get(i);
                
                // Add blank line between classes
                if (i > 0) {
                    context.blankLine();
                }
                
                visitReactiveClassDeclaration(rcd);
            }
        }
        
        // Format main declaration
        if (rebecaCode.getMainDeclaration() != null) {
            if (rebecaCode.getReactiveClassDeclaration() != null && 
                !rebecaCode.getReactiveClassDeclaration().isEmpty()) {
                context.blankLine();
            }
            visitMainDeclaration(rebecaCode.getMainDeclaration());
        }
    }
    
    /**
     * Format reactive class declaration
     */
    public void visitReactiveClassDeclaration(ReactiveClassDeclaration rcd) {
        context.isInClass = true;
        
        // Format class header (queue size will be added as literal if needed)
        context.append("reactiveclass ");
        context.append(rcd.getName());
        context.append(" {");
        context.newLine();
        context.increaseIndent();
        
        // Format known rebecs section
        if (rcd.getKnownRebecs() != null && !rcd.getKnownRebecs().isEmpty()) {
            visitKnownRebecs(rcd.getKnownRebecs());
            context.blankLine();
        }
        
        // Format state variables section
        if (rcd.getStatevars() != null && !rcd.getStatevars().isEmpty()) {
            visitStatevars(rcd.getStatevars());
            context.blankLine();
        }
        
        // Format constructors if available
        if (rcd.getConstructors() != null && !rcd.getConstructors().isEmpty()) {
            for (int i = 0; i < rcd.getConstructors().size(); i++) {
                MethodDeclaration constructor = rcd.getConstructors().get(i);
                visitMethodDeclaration(constructor);
                if (i < rcd.getConstructors().size() - 1) {
                    context.blankLine();
                }
            }
            context.blankLine();
        }
        
        // Format message servers (methods)
        if (rcd.getMsgsrvs() != null && !rcd.getMsgsrvs().isEmpty()) {
            for (int i = 0; i < rcd.getMsgsrvs().size(); i++) {
                MethodDeclaration method = rcd.getMsgsrvs().get(i);
                
                // Add blank line between methods
                if (i > 0) {
                    context.blankLine();
                }
                
                visitMethodDeclaration(method);
            }
        }
        
        context.decreaseIndent();
        context.appendLine("}");
        context.isInClass = false;
    }
    
    /**
     * Format known rebecs section
     */
    public void visitKnownRebecs(List<FieldDeclaration> knownRebecs) {
        context.appendLine("knownrebecs {");
        context.increaseIndent();
        
        for (FieldDeclaration field : knownRebecs) {
            visitFieldDeclaration(field);
        }
        
        context.decreaseIndent();
        context.appendLine("}");
    }
    
    /**
     * Format state variables section
     */
    public void visitStatevars(List<FieldDeclaration> statevars) {
        context.appendLine("statevars {");
        context.increaseIndent();
        
        for (FieldDeclaration field : statevars) {
            visitFieldDeclaration(field);
        }
        
        context.decreaseIndent();
        context.appendLine("}");
    }
    
    /**
     * Format field declaration
     */
    public void visitFieldDeclaration(FieldDeclaration field) {
        context.indent();
        
        // Type
        if (field.getType() != null) {
            visitType(field.getType());
            context.append(" ");
        }
        
        // Variable declarators
        if (field.getVariableDeclarators() != null) {
            for (int i = 0; i < field.getVariableDeclarators().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitVariableDeclarator(field.getVariableDeclarators().get(i));
            }
        }
        
        context.append(";");
        context.newLine();
    }
    
    /**
     * Format type
     */
    public void visitType(Type type) {
        context.append(type.getTypeName());
        // Note: Array dimensions will be handled at the parsing level if available
    }
    
    /**
     * Format variable declarator
     */
    public void visitVariableDeclarator(VariableDeclarator var) {
        context.append(var.getVariableName());
        // Note: Variable initializers will be handled at the parsing level if available
    }
    
    /**
     * Format method declaration
     */
    public void visitMethodDeclaration(MethodDeclaration method) {
        context.isInMethod = true;
        
        context.indent();
        context.append("msgsrv ");
        context.append(method.getName());
        context.append("()");
        
        // Basic method body structure
        context.append(" {");
        context.newLine();
        context.increaseIndent();
        
        // Add basic placeholder for method body
        context.appendLine("// Method body");
        
        context.decreaseIndent();
        context.appendLine("}");
        
        context.isInMethod = false;
    }
    
    
    /**
     * Format main declaration
     */
    public void visitMainDeclaration(MainDeclaration main) {
        context.isInMainSection = true;
        
        context.appendLine("main {");
        context.increaseIndent();
        
        if (main.getMainRebecDefinition() != null) {
            for (MainRebecDefinition mrd : main.getMainRebecDefinition()) {
                visitMainRebecDefinition(mrd);
            }
        }
        
        context.decreaseIndent();
        context.appendLine("}");
        
        context.isInMainSection = false;
    }
    
    /**
     * Format main rebec definition (instantiation)
     */
    public void visitMainRebecDefinition(MainRebecDefinition mrd) {
        context.indent();
        
        // Type
        if (mrd.getType() != null) {
            visitType(mrd.getType());
            context.append(" ");
        }
        
        // Name
        context.append(mrd.getName());
        
        // Basic instantiation syntax (specific bindings/arguments would need detailed API analysis)
        context.append("():();");
        context.newLine();
    }
}
