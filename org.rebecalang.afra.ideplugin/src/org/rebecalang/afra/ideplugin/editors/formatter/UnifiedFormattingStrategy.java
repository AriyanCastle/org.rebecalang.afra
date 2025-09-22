package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * Unified formatting strategy that uses AST-based formatters when available,
 * falling back to basic formatters when AST parsing fails.
 */
public class UnifiedFormattingStrategy implements IFormattingStrategy {
    
    private IAfraFormatter currentFormatter;
    private String currentFileExtension;
    
    @Override
    public void formatterStarts(String initialIndentation) {
        // Determine the file type from the active editor
        try {
            IEditorInput editorInput = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage()
                .getActiveEditor()
                .getEditorInput();
                
            if (editorInput instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) editorInput).getFile();
                currentFileExtension = file.getFileExtension();
                
                // Try to create AST-based formatter first
                currentFormatter = ASTFormatterFactory.getInstance().createFormatter(file);
                
                // If AST formatter creation fails, use fallback
                if (currentFormatter == null) {
                    currentFormatter = ASTFormatterFactory.getInstance()
                        .getFallbackFormatter(currentFileExtension);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to determine file type for formatting: " + e.getMessage());
            currentFormatter = null;
            currentFileExtension = null;
        }
        
        // Final fallback to basic Rebeca formatter
        if (currentFormatter == null) {
            currentFormatter = new FixedRebecaFormatter();
        }
    }
    
    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        if (currentFormatter == null) {
            return content;
        }
        
        try {
            // For the IFormattingStrategy interface, we create a temporary document
            // This is a simplified approach - in a real implementation, you might want
            // to pass the actual document from the editor
            TempDocument tempDoc = new TempDocument(content);
            return currentFormatter.format(tempDoc);
        } catch (Exception e) {
            System.err.println("Formatting failed: " + e.getMessage());
            e.printStackTrace();
            
            // Try fallback formatter if AST formatting fails
            try {
                IAfraFormatter fallback = ASTFormatterFactory.getInstance()
                    .getFallbackFormatter(currentFileExtension);
                if (fallback != null && fallback != currentFormatter) {
                    TempDocument tempDoc = new TempDocument(content);
                    return fallback.format(tempDoc);
                }
            } catch (Exception e2) {
                System.err.println("Fallback formatting also failed: " + e2.getMessage());
            }
            
            // Return original content if all formatting attempts fail
            return content;
        }
    }
    
    @Override
    public void formatterStops() {
        currentFormatter = null;
        currentFileExtension = null;
    }
    
    /**
     * Temporary document implementation for use with IAfraFormatter interface
     */
    private static class TempDocument implements IDocument {
        private String content;
        
        public TempDocument(String content) {
            this.content = content;
        }
        
        @Override
        public String get() {
            return content;
        }
        
        @Override
        public String get(int offset, int length) {
            return content.substring(offset, offset + length);
        }
        
        @Override
        public int getLength() {
            return content.length();
        }
        
        // Minimal implementation - other methods not needed for formatting
        @Override public char getChar(int offset) { return content.charAt(offset); }
        @Override public void addDocumentListener(org.eclipse.jface.text.IDocumentListener listener) {}
        @Override public void removeDocumentListener(org.eclipse.jface.text.IDocumentListener listener) {}
        @Override public void addPrenotifiedDocumentListener(org.eclipse.jface.text.IDocumentListener documentListener) {}
        @Override public void removePrenotifiedDocumentListener(org.eclipse.jface.text.IDocumentListener documentListener) {}
        @Override public void addPositionCategory(String category) {}
        @Override public void removePositionCategory(String category) throws org.eclipse.jface.text.BadPositionCategoryException {}
        @Override public String[] getPositionCategories() { return new String[0]; }
        @Override public boolean containsPositionCategory(String category) { return false; }
        @Override public void addPosition(org.eclipse.jface.text.Position position) {}
        @Override public void removePosition(org.eclipse.jface.text.Position position) {}
        @Override public void addPosition(String category, org.eclipse.jface.text.Position position) {}
        @Override public void removePosition(String category, org.eclipse.jface.text.Position position) {}
        @Override public org.eclipse.jface.text.Position[] getPositions(String category) { return new org.eclipse.jface.text.Position[0]; }
        @Override public org.eclipse.jface.text.ITypedRegion[] computePartitioning(int offset, int length) { return new org.eclipse.jface.text.ITypedRegion[0]; }
        @Override public void addDocumentPartitioningListener(org.eclipse.jface.text.IDocumentPartitioningListener listener) {}
        @Override public void removeDocumentPartitioningListener(org.eclipse.jface.text.IDocumentPartitioningListener listener) {}
        @Override public void setDocumentPartitioner(org.eclipse.jface.text.IDocumentPartitioner partitioner) {}
        @Override public org.eclipse.jface.text.IDocumentPartitioner getDocumentPartitioner() { return null; }
        @Override public int getLineLength(int line) { return 0; }
        @Override public int getLineOfOffset(int offset) { return 0; }
        @Override public int getLineOffset(int line) { return 0; }
        @Override public org.eclipse.jface.text.IRegion getLineInformation(int line) { return null; }
        @Override public org.eclipse.jface.text.IRegion getLineInformationOfOffset(int offset) { return null; }
        @Override public int getNumberOfLines() { return 0; }
        @Override public int getNumberOfLines(int offset, int length) { return 0; }
        @Override public int computeNumberOfLines(String text) { return 0; }
        @Override public String[] getLegalContentTypes() { return new String[0]; }
        @Override public String getContentType(int offset) { return ""; }
        @Override public org.eclipse.jface.text.ITypedRegion getPartition(int offset) { return null; }
        @Override public int search(int startOffset, String findString, boolean forwardSearch, boolean caseSensitive, boolean wholeWord) { return -1; }
        @Override public void replace(int offset, int length, String text) {}
        @Override public void set(String text) { this.content = text; }
    }
}
