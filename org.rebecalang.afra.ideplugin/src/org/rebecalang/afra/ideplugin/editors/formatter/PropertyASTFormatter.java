package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.propertycompiler.generalrebeca.objectmodel.PropertyModel;
import org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.LTLDefinition;
import org.rebecalang.compiler.propertycompiler.PropertyCompiler;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.rmc.RMCConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * AST-based formatter specifically for .property files
 */
public class PropertyASTFormatter {
    
    @Autowired
    private PropertyCompiler propertyCompiler;
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    public PropertyASTFormatter() {
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("Failed to initialize property compiler components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Format property content using AST if possible
     */
    public String formatPropertyContent(String content, ASTBasedFormatter.FormattingContext context) {
        try {
            // Try to parse with property compiler
            PropertyModel model = parsePropertyFile(content);
            if (model != null) {
                return formatWithPropertyAST(model, context);
            } else {
                // Fallback to structure-based formatting
                return formatPropertyStructure(content, context);
            }
        } catch (Exception e) {
            System.err.println("Property AST formatting failed, using structure-based: " + e.getMessage());
            return formatPropertyStructure(content, context);
        }
    }
    
    /**
     * Parse property file to get AST
     */
    private PropertyModel parsePropertyFile(String content) {
        try {
            if (propertyCompiler == null) {
                return null;
            }
            
            // Create temporary file
            File tempFile = File.createTempFile("formatter", ".property");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }
            
            // Parse property file
            Set<CompilerExtension> extensions = java.util.Collections.emptySet();
            CoreVersion version = CoreVersion.CORE_2_1;
            
            // Note: The actual API might be different, this is based on the compiler structure
            // PropertyModel model = propertyCompiler.compilePropertyFile(tempFile, extensions, version);
            
            // Clean up
            tempFile.delete();
            
            // For now, return null to use structure-based formatting
            return null;
            
        } catch (Exception e) {
            System.err.println("Failed to parse property file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Format using property AST
     */
    private String formatWithPropertyAST(PropertyModel model, ASTBasedFormatter.FormattingContext context) {
        // Reset context
        context.output = new StringBuilder();
        context.indentLevel = 0;
        
        // Format property block
        context.appendLine("property {");
        context.increaseIndent();
        
        // Format different sections based on AST
        if (model instanceof org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) {
            org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel coreModel = 
                (org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) model;
            
            // Format define section if present
            if (coreModel.getDefinitions() != null && !coreModel.getDefinitions().isEmpty()) {
                context.blankLine();
                context.appendLine("define {");
                context.increaseIndent();
                
                // Format definitions
                for (Object def : coreModel.getDefinitions()) {
                    // Format each definition
                    context.appendLine(def.toString() + ";");
                }
                
                context.decreaseIndent();
                context.appendLine("}");
            }
            
            // Format LTL section if present
            if (coreModel.getLTLDefinitions() != null && !coreModel.getLTLDefinitions().isEmpty()) {
                context.blankLine();
                context.appendLine("LTL {");
                context.increaseIndent();
                
                for (LTLDefinition ltl : coreModel.getLTLDefinitions()) {
                    context.appendLine(ltl.getName() + ": [LTL Expression];");
                }
                
                context.decreaseIndent();
                context.appendLine("}");
            }
            
            // Format Assertion section if present
            // Similar pattern for other sections...
        }
        
        context.decreaseIndent();
        context.appendLine("}");
        
        return context.output.toString();
    }
    
    /**
     * Structure-based property formatting (fallback)
     */
    private String formatPropertyStructure(String content, ASTBasedFormatter.FormattingContext context) {
        // Reset context
        context.output = new StringBuilder();
        context.indentLevel = 0;
        
        String[] lines = content.split("\\r?\\n");
        boolean inPropertyBlock = false;
        boolean inDefineBlock = false;
        boolean inAssertionBlock = false;
        boolean inLTLBlock = false;
        boolean needsBlankLine = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip empty lines (we'll add them back strategically)
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Check for property block start
            if (trimmed.startsWith("property")) {
                if (context.output.length() > 0) {
                    context.blankLine();
                }
                context.appendLine("property {");
                context.increaseIndent();
                inPropertyBlock = true;
                needsBlankLine = false;
                continue;
            }
            
            // Check for block endings
            if (trimmed.equals("}")) {
                context.decreaseIndent();
                context.appendLine("}");
                
                if (inDefineBlock || inAssertionBlock || inLTLBlock) {
                    needsBlankLine = true;
                }
                
                inDefineBlock = false;
                inAssertionBlock = false;
                inLTLBlock = false;
                continue;
            }
            
            // Check for define block
            if (trimmed.startsWith("define")) {
                if (needsBlankLine || (inPropertyBlock && (inAssertionBlock || inLTLBlock))) {
                    context.blankLine();
                }
                context.appendLine("define {");
                context.increaseIndent();
                inDefineBlock = true;
                needsBlankLine = false;
                continue;
            }
            
            // Check for Assertion block
            if (trimmed.startsWith("Assertion")) {
                if (needsBlankLine || inDefineBlock) {
                    context.blankLine();
                }
                context.appendLine("Assertion {");
                context.increaseIndent();
                inAssertionBlock = true;
                needsBlankLine = false;
                continue;
            }
            
            // Check for LTL block
            if (trimmed.startsWith("LTL")) {
                if (needsBlankLine || inDefineBlock || inAssertionBlock) {
                    context.blankLine();
                }
                context.appendLine("LTL {");
                context.increaseIndent();
                inLTLBlock = true;
                needsBlankLine = false;
                continue;
            }
            
            // Regular content line - format with proper spacing
            String formattedLine = formatPropertyLine(trimmed);
            context.appendLine(formattedLine);
        }
        
        // Close any remaining blocks
        while (context.indentLevel > 0) {
            context.decreaseIndent();
            context.appendLine("}");
        }
        
        return context.output.toString();
    }
    
    /**
     * Format individual property lines with proper spacing
     */
    private String formatPropertyLine(String line) {
        // Clean up spacing around operators and symbols
        line = line.replaceAll("\\s*=\\s*", " = ");
        line = line.replaceAll("\\s*:\\s*", ": ");
        line = line.replaceAll("\\s*&&\\s*", " && ");
        line = line.replaceAll("\\s*\\|\\|\\s*", " || ");
        line = line.replaceAll("\\s*!\\s*", "!");
        line = line.replaceAll("\\s*\\(\\s*", "(");
        line = line.replaceAll("\\s*\\)\\s*", ")");
        line = line.replaceAll("\\s*,\\s*", ", ");
        
        // Ensure proper semicolon formatting
        if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
            line += ";";
        }
        
        return line;
    }
}
