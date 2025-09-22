package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.core.resources.IFile;

/**
 * Factory for creating AST-based formatters for different file types
 */
public class ASTFormatterFactory {
    
    private static ASTFormatterFactory instance;
    
    private ASTFormatterFactory() {
    }
    
    public static ASTFormatterFactory getInstance() {
        if (instance == null) {
            instance = new ASTFormatterFactory();
        }
        return instance;
    }
    
    /**
     * Create the appropriate AST-based formatter for the given file
     * @param file The file to format
     * @return The appropriate formatter, or null if no AST formatter is available
     */
    public IAfraFormatter createFormatter(IFile file) {
        if (file == null) {
            return null;
        }
        
        String extension = file.getFileExtension();
        if (extension == null) {
            return null;
        }
        
        switch (extension.toLowerCase()) {
            case "rebeca":
                return new ASTBasedRebecaFormatter();
            case "property":
                return new ASTBasedPropertyFormatter();
            default:
                return null;
        }
    }
    
    /**
     * Create the appropriate AST-based formatter for the given file extension
     * @param fileExtension The file extension (without the dot)
     * @return The appropriate formatter, or null if no AST formatter is available
     */
    public IAfraFormatter createFormatter(String fileExtension) {
        if (fileExtension == null) {
            return null;
        }
        
        switch (fileExtension.toLowerCase()) {
            case "rebeca":
                return new ASTBasedRebecaFormatter();
            case "property":
                return new ASTBasedPropertyFormatter();
            default:
                return null;
        }
    }
    
    /**
     * Get a fallback formatter for the given file extension
     * @param fileExtension The file extension (without the dot)
     * @return A fallback formatter that doesn't use AST
     */
    public IAfraFormatter getFallbackFormatter(String fileExtension) {
        if (fileExtension == null) {
            return null;
        }
        
        switch (fileExtension.toLowerCase()) {
            case "rebeca":
                return new FixedRebecaFormatter();
            case "property":
                return new FixedPropertyFormatter();
            default:
                return null;
        }
    }
    
    /**
     * Check if AST-based formatting is supported for the given file extension
     * @param fileExtension The file extension (without the dot)
     * @return true if AST-based formatting is supported
     */
    public boolean isASTFormattingSupported(String fileExtension) {
        return "rebeca".equalsIgnoreCase(fileExtension) || 
               "property".equalsIgnoreCase(fileExtension);
    }
}
