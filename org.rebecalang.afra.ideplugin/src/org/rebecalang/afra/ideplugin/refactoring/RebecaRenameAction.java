package org.rebecalang.afra.ideplugin.refactoring;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for renaming Rebeca symbols (Ctrl+Shift+R)
 */
public class RebecaRenameAction extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof ITextEditor)) {
            return null;
        }
        
        ITextEditor textEditor = (ITextEditor) editor;
        Shell shell = textEditor.getSite().getShell();
        
        try {
            // Get current selection/cursor position
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            if (!(selection instanceof ITextSelection)) {
                showError(shell, "Please place cursor on a symbol to rename.");
                return null;
            }
            
            ITextSelection textSelection = (ITextSelection) selection;
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            
            // Get the file and project
            IFile file = (IFile) textEditor.getEditorInput().getAdapter(IFile.class);
            if (file == null) {
                showError(shell, "Could not determine current file.");
                return null;
            }
            
            IProject project = file.getProject();
            
            // Analyze symbol at cursor position
            SymbolAnalysisResult analysisResult = analyzeSymbolAtPosition(document, textSelection.getOffset());
            if (analysisResult == null) {
                showError(shell, "No renameable symbol found at cursor position.");
                return null;
            }
            
            // Find all occurrences of the symbol
            RebecaRefactoringParticipant refactoring = new RebecaRefactoringParticipant(project);
            List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences = refactoring.findAllOccurrences(
                analysisResult.symbolName, 
                analysisResult.symbolType, 
                file, 
                textSelection.getOffset()
            );
            
            if (occurrences.isEmpty()) {
                showError(shell, "No occurrences found for symbol '" + analysisResult.symbolName + "'.");
                return null;
            }
            
            // Show rename dialog
            RebecaRenameDialog dialog = new RebecaRenameDialog(shell, analysisResult.symbolName, 
                                                              analysisResult.symbolType, occurrences);
            
            if (dialog.open() == Window.OK) {
                // Perform the rename operation
                performRename(shell, dialog.getNewName(), dialog.getSelectedOccurrences());
            }
            
        } catch (Exception e) {
            showError(shell, "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Analyze the symbol at the given position
     */
    private SymbolAnalysisResult analyzeSymbolAtPosition(IDocument document, int offset) {
        try {
            // Get the word at the cursor position
            IRegion wordRegion = getWordRegion(document, offset);
            if (wordRegion == null) {
                return null;
            }
            
            String word = document.get(wordRegion.getOffset(), wordRegion.getLength());
            if (word.trim().isEmpty()) {
                return null;
            }
            
        // Determine symbol type based on context
        RebecaRefactoringParticipant.SymbolType symbolType = determineSymbolType(document, wordRegion.getOffset(), word);
        if (symbolType == null) {
            return null;
        }
        
        return new SymbolAnalysisResult(word, symbolType);
            
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    /**
     * Get the word region at the given offset
     */
    private IRegion getWordRegion(IDocument document, int offset) throws BadLocationException {
        int start = offset;
        int end = offset;
        
        // Move start backwards
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            start--;
        }
        
        // Move end forwards
        while (end < document.getLength()) {
            char c = document.getChar(end);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            end++;
        }
        
        if (end > start) {
            return new org.eclipse.jface.text.Region(start, end - start);
        }
        
        return null;
    }
    
    /**
     * Determine the type of symbol based on context
     */
    private RebecaRefactoringParticipant.SymbolType determineSymbolType(IDocument document, int offset, String word) {
        try {
            String content = document.get();
            
            // Get surrounding context
            int lineStart = document.getLineOffset(document.getLineOfOffset(offset));
            int lineEnd = lineStart + document.getLineLength(document.getLineOfOffset(offset));
            String line = content.substring(lineStart, lineEnd);
            
            int positionInLine = offset - lineStart;
            
            // Check for class declaration: reactiveclass ClassName
            if (line.matches(".*\\breactiveclass\\s+" + java.util.regex.Pattern.quote(word) + "\\b.*")) {
                return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
            }
            
            // Check for method declaration: msgsrv methodName
            if (line.matches(".*\\bmsgsrv\\s+" + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            // Check for method call: something.methodName(
            if (line.matches(".*\\w+\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*") ||
                line.matches(".*\\bself\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            // Check for class usage in main block or knownrebecs
            String beforeWord = line.substring(0, positionInLine);
            String afterWord = line.substring(positionInLine + word.length());
            
            if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+\\w+.*")) {
                // Pattern: ClassName instanceName
                return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
            }
            
            // Check if it's in knownrebecs section
            if (isInSection(content, offset, "knownrebecs")) {
                // If it's at the beginning of line and followed by variable names, it's a class name
                if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+[\\w\\s,]+;.*")) {
                    // Pattern: ClassName varName1, varName2, ...;
                    return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
                } else {
                    // Otherwise it's an instance name
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
            }
            
            // Check if it's in statevars section (variable declaration)
            if (isInSection(content, offset, "statevars")) {
                return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            }
            
            // Check for property definition in .property files
            if (line.matches("\\s*" + java.util.regex.Pattern.quote(word) + "\\s*=.*")) {
                return RebecaRefactoringParticipant.SymbolType.PROPERTY_NAME;
            }
            
            // Default: treat as variable or instance name
            if (line.contains(".")) {
                // If it's part of object.field, likely an instance or variable
                if (beforeWord.endsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME; // field access
                } else if (afterWord.startsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME; // object access
                }
            }
            
            // Default to variable name for standalone identifiers
            return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    /**
     * Check if the offset is within a specific section (statevars, knownrebecs, etc.)
     */
    private boolean isInSection(String content, int offset, String sectionName) {
        // Find the nearest section before the offset
        String beforeOffset = content.substring(0, offset);
        
        int lastSectionStart = beforeOffset.lastIndexOf(sectionName);
        if (lastSectionStart == -1) {
            return false;
        }
        
        // Check if we're within the section's braces
        int braceStart = content.indexOf('{', lastSectionStart);
        if (braceStart == -1 || braceStart > offset) {
            return false;
        }
        
        // Find matching closing brace
        int braceCount = 1;
        int pos = braceStart + 1;
        while (pos < content.length() && braceCount > 0) {
            if (content.charAt(pos) == '{') {
                braceCount++;
            } else if (content.charAt(pos) == '}') {
                braceCount--;
            }
            pos++;
        }
        
        return offset < pos - 1;
    }
    
    /**
     * Perform the actual rename operation
     */
    private void performRename(Shell shell, String newName, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences) {
        try {
            // Group occurrences by file
            java.util.Map<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> fileOccurrences = new java.util.HashMap<>();
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                fileOccurrences.computeIfAbsent(occ.file, k -> new java.util.ArrayList<>()).add(occ);
            }
            
            // Process each file
            int totalRenamed = 0;
            for (java.util.Map.Entry<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> entry : fileOccurrences.entrySet()) {
                totalRenamed += renameInFile(entry.getKey(), entry.getValue(), newName);
            }
            
            // Show success message
            MessageDialog.openInformation(shell, "Rename Complete", 
                String.format("Successfully renamed %d occurrence(s) in %d file(s).", 
                            totalRenamed, fileOccurrences.size()));
            
        } catch (Exception e) {
            showError(shell, "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Rename occurrences in a single file
     */
    private int renameInFile(IFile file, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences, String newName) throws Exception {
        org.eclipse.ui.texteditor.IDocumentProvider provider = new org.eclipse.ui.editors.text.TextFileDocumentProvider();
        provider.connect(file);
        
        try {
            IDocument document = provider.getDocument(file);
            
            // Sort occurrences by offset in reverse order (to maintain offsets while replacing)
            occurrences.sort((a, b) -> Integer.compare(b.offset, a.offset));
            
            // Replace each occurrence
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                document.replace(occ.offset, occ.length, newName);
            }
            
            // Save the document
            provider.saveDocument(null, file, document, true);
            
            return occurrences.size();
            
        } finally {
            provider.disconnect(file);
        }
    }
    
    private void showError(Shell shell, String message) {
        MessageDialog.openError(shell, "Rename Error", message);
    }
    
    /**
     * Result of symbol analysis
     */
    private static class SymbolAnalysisResult {
        final String symbolName;
        final RebecaRefactoringParticipant.SymbolType symbolType;
        
        SymbolAnalysisResult(String symbolName, RebecaRefactoringParticipant.SymbolType symbolType) {
            this.symbolName = symbolName;
            this.symbolType = symbolType;
        }
    }
}
