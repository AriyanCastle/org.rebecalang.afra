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
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for renaming Rebeca symbols (Alt+Shift+R)
 */
public class RebecaRenameAction extends AbstractHandler {
    
    private Text inlineRenameText;
    private Shell inlineRenameShell;
    private String originalSymbolName;
    private SymbolAnalysisResult currentAnalysisResult;
    private ITextEditor currentEditor;
    private IDocument currentDocument;
    private IFile currentFile;
    private int currentOffset;
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof ITextEditor)) {
            return null;
        }
        
        currentEditor = (ITextEditor) editor;
        Shell shell = currentEditor.getSite().getShell();
        
        try {
            // Get current selection/cursor position
            ISelection selection = currentEditor.getSelectionProvider().getSelection();
            if (!(selection instanceof ITextSelection)) {
                showError(shell, "Please place cursor on a symbol to rename.");
                return null;
            }
            
            ITextSelection textSelection = (ITextSelection) selection;
            currentDocument = currentEditor.getDocumentProvider().getDocument(currentEditor.getEditorInput());
            currentOffset = textSelection.getOffset();
            
            // Get the file and project
            currentFile = (IFile) currentEditor.getEditorInput().getAdapter(IFile.class);
            if (currentFile == null) {
                showError(shell, "Could not determine current file.");
                return null;
            }
            
            // Analyze symbol at cursor position
            currentAnalysisResult = analyzeSymbolAtPosition(currentDocument, currentOffset);
            if (currentAnalysisResult == null) {
                showError(shell, "No renameable symbol found at cursor position.");
                return null;
            }
            
            originalSymbolName = currentAnalysisResult.symbolName;
            
            // Show inline rename widget
            showInlineRenameWidget();
            
        } catch (Exception e) {
            showError(shell, "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Show the inline rename widget near the cursor position
     */
    private void showInlineRenameWidget() {
        try {
            // Get the text viewer from the editor
            ISourceViewer sourceViewer = (ISourceViewer) currentEditor.getAdapter(ISourceViewer.class);
            if (sourceViewer == null) {
                return;
            }
            
            // Get cursor position in screen coordinates
            Point cursorLocation = getCursorScreenLocation(sourceViewer);
            if (cursorLocation == null) {
                return;
            }
            
            // Create a small shell for the inline text
            inlineRenameShell = new Shell(currentEditor.getSite().getShell(), SWT.NO_TRIM | SWT.ON_TOP);
            inlineRenameShell.setLayout(new org.eclipse.swt.layout.FillLayout());
            
            // Create the text widget
            inlineRenameText = new Text(inlineRenameShell, SWT.BORDER);
            inlineRenameText.setText(originalSymbolName);
            inlineRenameText.selectAll();
            
            // Calculate size and position
            Point textSize = inlineRenameText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            textSize.x = Math.max(textSize.x, originalSymbolName.length() * 8 + 20); // minimum width
            
            inlineRenameShell.setSize(textSize.x, textSize.y);
            inlineRenameShell.setLocation(cursorLocation.x, cursorLocation.y + 20); // slightly below cursor
            
            // Add event handlers
            setupInlineRenameEventHandlers();
            
            // Show the shell and focus the text
            inlineRenameShell.open();
            inlineRenameText.setFocus();
            
        } catch (Exception e) {
            e.printStackTrace();
            hideInlineRenameWidget();
        }
    }
    
    /**
     * Get the cursor location in screen coordinates
     */
    private Point getCursorScreenLocation(ISourceViewer sourceViewer) {
        try {
            // Get the text widget
            org.eclipse.swt.custom.StyledText textWidget = sourceViewer.getTextWidget();
            if (textWidget == null) {
                return null;
            }
            
            // Get current caret position in the text widget
            int caretOffset = textWidget.getCaretOffset();
            
            // Get the location of the caret in the text widget
            Point location = textWidget.getLocationAtOffset(caretOffset);
            
            // Convert to screen coordinates
            return textWidget.toDisplay(location);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Setup event handlers for the inline rename widget
     */
    private void setupInlineRenameEventHandlers() {
        // Handle Enter key to confirm rename
        inlineRenameText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    // Enter pressed - perform rename
                    performInlineRename();
                } else if (e.keyCode == SWT.ESC) {
                    // Escape pressed - cancel rename
                    hideInlineRenameWidget();
                }
            }
        });
        
        // Hide widget when focus is lost
        inlineRenameText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideInlineRenameWidget();
            }
        });
    }
    
    /**
     * Perform the actual rename operation when Enter is pressed
     */
    private void performInlineRename() {
        try {
            String newName = inlineRenameText.getText().trim();
            
            // Validate the new name
            if (newName.isEmpty() || newName.equals(originalSymbolName)) {
                hideInlineRenameWidget();
                return;
            }
            
            if (!isValidName(newName)) {
                showError(inlineRenameShell, "Invalid identifier name: " + newName);
                return;
            }
            
            // Hide the widget first
            hideInlineRenameWidget();
            
            // Find all occurrences and perform rename
            IProject project = currentFile.getProject();
            RebecaRefactoringParticipant refactoring = new RebecaRefactoringParticipant(project);
            List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences = refactoring.findAllOccurrences(
                currentAnalysisResult.symbolName, 
                currentAnalysisResult.symbolType, 
                currentFile, 
                currentOffset
            );
            
            if (occurrences.isEmpty()) {
                showError(currentEditor.getSite().getShell(), "No occurrences found for symbol '" + originalSymbolName + "'.");
                return;
            }
            
            // Perform the rename operation
            performRename(currentEditor.getSite().getShell(), newName, occurrences);
            
        } catch (Exception e) {
            hideInlineRenameWidget();
            showError(currentEditor.getSite().getShell(), "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hide the inline rename widget
     */
    private void hideInlineRenameWidget() {
        if (inlineRenameShell != null && !inlineRenameShell.isDisposed()) {
            inlineRenameShell.dispose();
        }
        inlineRenameShell = null;
        inlineRenameText = null;
    }
    
    /**
     * Validate if the given name is a valid identifier
     */
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check if it's a valid identifier
        name = name.trim();
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        
        // Check if it's not a reserved keyword
        return !isReservedKeyword(name);
    }
    
    /**
     * Check if the name is a reserved keyword
     */
    private boolean isReservedKeyword(String name) {
        String[] keywords = {
            "reactiveclass", "msgsrv", "knownrebecs", "statevars", "main", 
            "if", "else", "while", "for", "true", "false", "self", "sender",
            "boolean", "int", "byte", "short", "long", "float", "double",
            "after", "deadline", "delay"
        };
        
        for (String keyword : keywords) {
            if (keyword.equals(name)) {
                return true;
            }
        }
        
        return false;
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
