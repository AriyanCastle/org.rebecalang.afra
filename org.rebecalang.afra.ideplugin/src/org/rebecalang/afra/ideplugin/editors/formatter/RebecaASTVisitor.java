package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.List;

import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ArrayInitializer;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ArrayLiteral;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.AssignmentStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.BinaryExpression;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.BlockStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.BooleanLiteral;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.BreakStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ContinueStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ConditionalStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ConstructorDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.DotPrimary;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Expression;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FieldDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ForStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FormalParameterDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.IntegerLiteral;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MethodDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.NonDetExpression;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReactiveClassDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaCode;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReturnStatement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Statement;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.StringLiteral;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.TernaryExpression;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Type;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.UnaryExpression;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableDeclarator;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableInitializer;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.WhileStatement;

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
        
        // Format class header with queue size
        context.append("reactiveclass ");
        context.append(rcd.getName());
        if (rcd.getQueueLength() != null && rcd.getQueueLength().getValue() != null) {
            context.append("(");
            context.append(rcd.getQueueLength().getValue());
            context.append(")");
        }
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
        
        // Format constructor
        if (rcd.getConstructors() != null && !rcd.getConstructors().isEmpty()) {
            for (ConstructorDeclaration constructor : rcd.getConstructors()) {
                visitConstructorDeclaration(constructor);
                context.blankLine();
            }
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
        
        // Handle array dimensions
        if (type.getDimensions() != null && type.getDimensions().size() > 0) {
            for (Expression dimension : type.getDimensions()) {
                context.append("[");
                if (dimension != null) {
                    visitExpression(dimension);
                }
                context.append("]");
            }
        }
    }
    
    /**
     * Format variable declarator
     */
    public void visitVariableDeclarator(VariableDeclarator var) {
        context.append(var.getVariableName());
        
        if (var.getVariableInitializer() != null) {
            context.append(" = ");
            visitVariableInitializer(var.getVariableInitializer());
        }
    }
    
    /**
     * Format variable initializer
     */
    public void visitVariableInitializer(VariableInitializer init) {
        if (init instanceof Expression) {
            visitExpression((Expression) init);
        } else if (init instanceof ArrayInitializer) {
            visitArrayInitializer((ArrayInitializer) init);
        }
    }
    
    /**
     * Format array initializer
     */
    public void visitArrayInitializer(ArrayInitializer init) {
        context.append("{");
        if (init.getVariableInitializers() != null) {
            for (int i = 0; i < init.getVariableInitializers().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitVariableInitializer(init.getVariableInitializers().get(i));
            }
        }
        context.append("}");
    }
    
    /**
     * Format constructor declaration
     */
    public void visitConstructorDeclaration(ConstructorDeclaration constructor) {
        context.indent();
        context.append(constructor.getName());
        context.append("(");
        
        // Parameters
        if (constructor.getFormalParameters() != null) {
            for (int i = 0; i < constructor.getFormalParameters().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitFormalParameterDeclaration(constructor.getFormalParameters().get(i));
            }
        }
        
        context.append(")");
        
        // Body
        if (constructor.getBlock() != null) {
            context.append(" {");
            context.newLine();
            context.increaseIndent();
            visitBlockStatement(constructor.getBlock());
            context.decreaseIndent();
            context.appendLine("}");
        } else {
            context.append(" {");
            context.newLine();
            context.appendLine("}");
        }
    }
    
    /**
     * Format method declaration
     */
    public void visitMethodDeclaration(MethodDeclaration method) {
        context.isInMethod = true;
        
        context.indent();
        context.append("msgsrv ");
        context.append(method.getName());
        context.append("(");
        
        // Parameters
        if (method.getFormalParameters() != null) {
            for (int i = 0; i < method.getFormalParameters().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitFormalParameterDeclaration(method.getFormalParameters().get(i));
            }
        }
        
        context.append(")");
        
        // Body
        if (method.getBlock() != null) {
            context.append(" {");
            context.newLine();
            context.increaseIndent();
            visitBlockStatement(method.getBlock());
            context.decreaseIndent();
            context.appendLine("}");
        } else {
            context.append(" {");
            context.newLine();
            context.appendLine("}");
        }
        
        context.isInMethod = false;
    }
    
    /**
     * Format formal parameter declaration
     */
    public void visitFormalParameterDeclaration(FormalParameterDeclaration param) {
        if (param.getType() != null) {
            visitType(param.getType());
            context.append(" ");
        }
        context.append(param.getName());
    }
    
    /**
     * Format block statement
     */
    public void visitBlockStatement(BlockStatement block) {
        if (block.getStatements() != null) {
            for (Statement stmt : block.getStatements()) {
                visitStatement(stmt);
            }
        }
    }
    
    /**
     * Format statement
     */
    public void visitStatement(Statement stmt) {
        if (stmt instanceof AssignmentStatement) {
            visitAssignmentStatement((AssignmentStatement) stmt);
        } else if (stmt instanceof ConditionalStatement) {
            visitConditionalStatement((ConditionalStatement) stmt);
        } else if (stmt instanceof ForStatement) {
            visitForStatement((ForStatement) stmt);
        } else if (stmt instanceof WhileStatement) {
            visitWhileStatement((WhileStatement) stmt);
        } else if (stmt instanceof BlockStatement) {
            visitBlockStatement((BlockStatement) stmt);
        } else if (stmt instanceof ReturnStatement) {
            visitReturnStatement((ReturnStatement) stmt);
        } else if (stmt instanceof BreakStatement) {
            visitBreakStatement((BreakStatement) stmt);
        } else if (stmt instanceof ContinueStatement) {
            visitContinueStatement((ContinueStatement) stmt);
        }
        // Add more statement types as needed
    }
    
    /**
     * Format assignment statement
     */
    public void visitAssignmentStatement(AssignmentStatement stmt) {
        context.indent();
        
        if (stmt.getLeft() != null) {
            visitExpression(stmt.getLeft());
        }
        
        context.append(" = ");
        
        if (stmt.getRight() != null) {
            visitExpression(stmt.getRight());
        }
        
        context.append(";");
        context.newLine();
    }
    
    /**
     * Format conditional statement (if/else)
     */
    public void visitConditionalStatement(ConditionalStatement stmt) {
        context.indent();
        context.append("if (");
        
        if (stmt.getCondition() != null) {
            visitExpression(stmt.getCondition());
        }
        
        context.append(")");
        
        // Then statement
        if (stmt.getStatement() != null) {
            if (stmt.getStatement() instanceof BlockStatement) {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitBlockStatement((BlockStatement) stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            } else {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitStatement(stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            }
        }
        
        // Else statement
        if (stmt.getElseStatement() != null) {
            context.append(" else");
            if (stmt.getElseStatement() instanceof ConditionalStatement) {
                context.append(" ");
                visitConditionalStatement((ConditionalStatement) stmt.getElseStatement());
            } else if (stmt.getElseStatement() instanceof BlockStatement) {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitBlockStatement((BlockStatement) stmt.getElseStatement());
                context.decreaseIndent();
                context.appendLine("}");
            } else {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitStatement(stmt.getElseStatement());
                context.decreaseIndent();
                context.appendLine("}");
            }
        }
    }
    
    /**
     * Format for statement
     */
    public void visitForStatement(ForStatement stmt) {
        context.indent();
        context.append("for (");
        
        // Initialization
        if (stmt.getForInit() != null) {
            visitStatement(stmt.getForInit());
        }
        context.append("; ");
        
        // Condition
        if (stmt.getCondition() != null) {
            visitExpression(stmt.getCondition());
        }
        context.append("; ");
        
        // Update
        if (stmt.getForUpdate() != null) {
            visitExpression(stmt.getForUpdate());
        }
        
        context.append(")");
        
        // Body
        if (stmt.getStatement() != null) {
            if (stmt.getStatement() instanceof BlockStatement) {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitBlockStatement((BlockStatement) stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            } else {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitStatement(stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            }
        }
    }
    
    /**
     * Format while statement
     */
    public void visitWhileStatement(WhileStatement stmt) {
        context.indent();
        context.append("while (");
        
        if (stmt.getCondition() != null) {
            visitExpression(stmt.getCondition());
        }
        
        context.append(")");
        
        // Body
        if (stmt.getStatement() != null) {
            if (stmt.getStatement() instanceof BlockStatement) {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitBlockStatement((BlockStatement) stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            } else {
                context.append(" {");
                context.newLine();
                context.increaseIndent();
                visitStatement(stmt.getStatement());
                context.decreaseIndent();
                context.appendLine("}");
            }
        }
    }
    
    /**
     * Format return statement
     */
    public void visitReturnStatement(ReturnStatement stmt) {
        context.indent();
        context.append("return");
        
        if (stmt.getExpression() != null) {
            context.append(" ");
            visitExpression(stmt.getExpression());
        }
        
        context.append(";");
        context.newLine();
    }
    
    /**
     * Format break statement
     */
    public void visitBreakStatement(BreakStatement stmt) {
        context.appendLine("break;");
    }
    
    /**
     * Format continue statement
     */
    public void visitContinueStatement(ContinueStatement stmt) {
        context.appendLine("continue;");
    }
    
    /**
     * Format expression
     */
    public void visitExpression(Expression expr) {
        if (expr instanceof BinaryExpression) {
            visitBinaryExpression((BinaryExpression) expr);
        } else if (expr instanceof UnaryExpression) {
            visitUnaryExpression((UnaryExpression) expr);
        } else if (expr instanceof TernaryExpression) {
            visitTernaryExpression((TernaryExpression) expr);
        } else if (expr instanceof DotPrimary) {
            visitDotPrimary((DotPrimary) expr);
        } else if (expr instanceof IntegerLiteral) {
            visitIntegerLiteral((IntegerLiteral) expr);
        } else if (expr instanceof BooleanLiteral) {
            visitBooleanLiteral((BooleanLiteral) expr);
        } else if (expr instanceof StringLiteral) {
            visitStringLiteral((StringLiteral) expr);
        } else if (expr instanceof ArrayLiteral) {
            visitArrayLiteral((ArrayLiteral) expr);
        } else if (expr instanceof NonDetExpression) {
            visitNonDetExpression((NonDetExpression) expr);
        }
        // Add more expression types as needed
    }
    
    /**
     * Format binary expression
     */
    public void visitBinaryExpression(BinaryExpression expr) {
        if (expr.getLeft() != null) {
            visitExpression(expr.getLeft());
        }
        
        context.append(" ");
        context.append(expr.getOperator());
        context.append(" ");
        
        if (expr.getRight() != null) {
            visitExpression(expr.getRight());
        }
    }
    
    /**
     * Format unary expression
     */
    public void visitUnaryExpression(UnaryExpression expr) {
        context.append(expr.getOperator());
        if (expr.getExpression() != null) {
            visitExpression(expr.getExpression());
        }
    }
    
    /**
     * Format ternary expression
     */
    public void visitTernaryExpression(TernaryExpression expr) {
        if (expr.getCondition() != null) {
            visitExpression(expr.getCondition());
        }
        
        context.append(" ? ");
        
        if (expr.getLeft() != null) {
            visitExpression(expr.getLeft());
        }
        
        context.append(" : ");
        
        if (expr.getRight() != null) {
            visitExpression(expr.getRight());
        }
    }
    
    /**
     * Format dot primary (method calls, field access)
     */
    public void visitDotPrimary(DotPrimary expr) {
        if (expr.getLeft() != null) {
            visitExpression(expr.getLeft());
        }
        
        context.append(".");
        context.append(expr.getRight());
        
        // Handle method call parentheses and arguments
        if (expr.getArguments() != null) {
            context.append("(");
            for (int i = 0; i < expr.getArguments().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitExpression(expr.getArguments().get(i));
            }
            context.append(")");
        }
    }
    
    /**
     * Format integer literal
     */
    public void visitIntegerLiteral(IntegerLiteral expr) {
        context.append(expr.getValue());
    }
    
    /**
     * Format boolean literal
     */
    public void visitBooleanLiteral(BooleanLiteral expr) {
        context.append(expr.getValue() ? "true" : "false");
    }
    
    /**
     * Format string literal
     */
    public void visitStringLiteral(StringLiteral expr) {
        context.append("\"");
        context.append(expr.getValue());
        context.append("\"");
    }
    
    /**
     * Format array literal
     */
    public void visitArrayLiteral(ArrayLiteral expr) {
        context.append("[");
        if (expr.getIndexes() != null) {
            for (int i = 0; i < expr.getIndexes().size(); i++) {
                if (i > 0) {
                    context.append("][");
                }
                visitExpression(expr.getIndexes().get(i));
            }
        }
        context.append("]");
    }
    
    /**
     * Format non-deterministic expression
     */
    public void visitNonDetExpression(NonDetExpression expr) {
        context.append("?(");
        if (expr.getChoices() != null) {
            for (int i = 0; i < expr.getChoices().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitExpression(expr.getChoices().get(i));
            }
        }
        context.append(")");
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
        
        // Bindings (known rebecs)
        if (mrd.getBindings() != null && !mrd.getBindings().isEmpty()) {
            context.append("(");
            for (int i = 0; i < mrd.getBindings().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitExpression(mrd.getBindings().get(i));
            }
            context.append(")");
        }
        
        // Arguments (constructor parameters)
        context.append(":(");
        if (mrd.getArguments() != null) {
            for (int i = 0; i < mrd.getArguments().size(); i++) {
                if (i > 0) {
                    context.append(", ");
                }
                visitExpression(mrd.getArguments().get(i));
            }
        }
        context.append(");");
        
        context.newLine();
    }
}
