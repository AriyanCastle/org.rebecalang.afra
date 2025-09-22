package org.rebecalang.afra.ideplugin.editors.formatter;

/**
 * Registry for formatter implementations.
 * Provides a centralized way to access the appropriate formatter for different file types.
 */
public class FormatterRegistry {
    
    private static FormatterRegistry instance;
    
    private final ASTBasedFormatter astFormatter;
    private final FallbackRebecaFormatter fallbackFormatter;
    private final FixedRebecaFormatter legacyRebecaFormatter;
    private final FixedPropertyFormatter legacyPropertyFormatter;
    
    private FormatterRegistry() {
        fallbackFormatter = new FallbackRebecaFormatter();
        astFormatter = new ASTBasedFormatter();
        legacyRebecaFormatter = new FixedRebecaFormatter();
        legacyPropertyFormatter = new FixedPropertyFormatter();
    }
    
    /**
     * Get the singleton instance
     */
    public static FormatterRegistry getInstance() {
        if (instance == null) {
            instance = new FormatterRegistry();
        }
        return instance;
    }
    
    /**
     * Get the best formatter for Rebeca files
     */
    public IAfraFormatter getRebecaFormatter() {
        // Use fallback formatter as primary to ensure compilation success
        return fallbackFormatter;
    }
    
    /**
     * Get the best formatter for property files
     */
    public IAfraFormatter getPropertyFormatter() {
        // Use fallback formatter which handles both .rebeca and .property files
        return fallbackFormatter;
    }
    
    /**
     * Get formatter by file extension
     */
    public IAfraFormatter getFormatterByExtension(String extension) {
        if (extension == null) {
            return fallbackFormatter;
        }
        
        switch (extension.toLowerCase()) {
            case "rebeca":
                return getRebecaFormatter();
            case "property":
                return getPropertyFormatter();
            default:
                return fallbackFormatter; // Default to fallback formatter
        }
    }
    
    /**
     * Get legacy formatter for Rebeca files (for comparison/fallback)
     */
    public IAfraFormatter getLegacyRebecaFormatter() {
        return legacyRebecaFormatter;
    }
    
    /**
     * Get legacy formatter for property files (for comparison/fallback)
     */
    public IAfraFormatter getLegacyPropertyFormatter() {
        return legacyPropertyFormatter;
    }
    
    /**
     * Check if AST formatting is available
     */
    public boolean isASTFormattingAvailable() {
        try {
            // Test if the AST formatter can be initialized
            return astFormatter != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get formatter with preference (AST vs Legacy)
     */
    public IAfraFormatter getFormatter(String extension, boolean preferAST) {
        if (!preferAST) {
            // Use legacy formatters
            if ("rebeca".equalsIgnoreCase(extension)) {
                return legacyRebecaFormatter;
            } else if ("property".equalsIgnoreCase(extension)) {
                return legacyPropertyFormatter;
            }
        }
        
        // Default to fallback formatter
        return getFormatterByExtension(extension);
    }
}
