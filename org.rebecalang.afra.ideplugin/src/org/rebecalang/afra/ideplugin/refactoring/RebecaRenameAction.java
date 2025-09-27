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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

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
            ISelection selection = currentEditor.getSelectionProvider().getSelection();
            
            if (!(selection instanceof ITextSelection)) {
                showError(shell, "Please place cursor on a symbol to rename.");
                return null;
            }
            
            ITextSelection textSelection = (ITextSelection) selection;
            currentDocument = currentEditor.getDocumentProvider().getDocument(currentEditor.getEditorInput());
            currentOffset = textSelection.getOffset();
            
            currentFile = (IFile) currentEditor.getEditorInput().getAdapter(IFile.class);
            if (currentFile == null) {
                showError(shell, "Could not determine current file.");
                return null;
            }
            
            currentAnalysisResult = analyzeSymbolAtPosition(currentDocument, currentOffset);
            
            if (currentAnalysisResult == null) {
                showError(shell, "No renameable symbol found at cursor position.");
                return null;
            }
            
            originalSymbolName = currentAnalysisResult.symbolName;

            showInlineRenameWidget();
            
        } catch (Exception e) {
            System.out.println("[RebecaRename] Exception in execute: " + e.getMessage());
            e.printStackTrace();
            showError(shell, "Rename operation failed: " + e.getMessage());
        }
        
        return null;
    }

    private void showInlineRenameWidget() {
        try {
            ISourceViewer sourceViewer = getSourceViewerFromEditor(currentEditor);
            
            if (sourceViewer == null) {
                showInlineRenameWidgetAlternative();
                return;
            }
            
            Point cursorLocation = getCursorScreenLocation(sourceViewer);
            
            if (cursorLocation == null) {
                return;
            }
            
            inlineRenameShell = new Shell(currentEditor.getSite().getShell(), SWT.NO_TRIM | SWT.ON_TOP);
            inlineRenameShell.setLayout(new org.eclipse.swt.layout.FillLayout());

            inlineRenameText = new Text(inlineRenameShell, SWT.BORDER);
            inlineRenameText.setText(originalSymbolName);
            inlineRenameText.selectAll();
            
            Point textSize = inlineRenameText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            textSize.x = Math.max(textSize.x, originalSymbolName.length() * 8 + 20);
            
            inlineRenameShell.setSize(textSize.x, textSize.y);
            inlineRenameShell.setLocation(cursorLocation.x, cursorLocation.y + 20);

            setupInlineRenameEventHandlers();
            
            inlineRenameShell.open();
            inlineRenameText.setFocus();
            
        } catch (Exception e) {
            System.out.println("[RebecaRename] Exception in showInlineRenameWidget: " + e.getMessage());
            e.printStackTrace();
            hideInlineRenameWidget();
        }
    }
    
    private ISourceViewer getSourceViewerFromEditor(ITextEditor editor) {
        
        ISourceViewer sourceViewer = (ISourceViewer) editor.getAdapter(ISourceViewer.class);
        if (sourceViewer != null) {
            return sourceViewer;
        }
        
        try {
            if (editor instanceof org.eclipse.ui.texteditor.AbstractTextEditor) {
                
                java.lang.reflect.Method method = org.eclipse.ui.texteditor.AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
                method.setAccessible(true);
                Object result = method.invoke(editor);
                
                if (result instanceof ISourceViewer) {
                    return (ISourceViewer) result;
                }
            }
        } catch (Exception e) {
            System.out.println("[RebecaRename] Reflection approach failed: " + e.getMessage());
        }
        
        try {
            java.lang.reflect.Method method = editor.getClass().getMethod("getSourceViewer");
            Object result = method.invoke(editor);
            if (result instanceof ISourceViewer) {
                return (ISourceViewer) result;
            }
        } catch (Exception e) {
            System.out.println("[RebecaRename] Editor method approach failed: " + e.getMessage());
        }
        
        return null;
    }

    private void showInlineRenameWidgetAlternative() {
        try {
            org.eclipse.swt.widgets.Control editorControl = (org.eclipse.swt.widgets.Control) currentEditor.getAdapter(org.eclipse.swt.widgets.Control.class);
            if (editorControl == null) {
                showInlineRenameWidgetAtCenter();
                return;
            }
                        
            org.eclipse.swt.graphics.Rectangle bounds = editorControl.getBounds();
            org.eclipse.swt.graphics.Point location = editorControl.toDisplay(bounds.x + 100, bounds.y + 100);
            
            createInlineRenameWidget(location);
            
        } catch (Exception e) {
            System.out.println("[RebecaRename] Alternative approach failed: " + e.getMessage());
            e.printStackTrace();
            showInlineRenameWidgetAtCenter();
        }
    }
    
    private void showInlineRenameWidgetAtCenter() {
        try {
            Shell parentShell = currentEditor.getSite().getShell();
            org.eclipse.swt.graphics.Rectangle bounds = parentShell.getBounds();
            
            org.eclipse.swt.graphics.Point location = new org.eclipse.swt.graphics.Point(
                bounds.x + bounds.width / 2, 
                bounds.y + bounds.height / 2
            );
            
            createInlineRenameWidget(location);
            
        } catch (Exception e) {
            System.out.println("[RebecaRename] Center positioning failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createInlineRenameWidget(org.eclipse.swt.graphics.Point location) {
        try {
            inlineRenameShell = new Shell(currentEditor.getSite().getShell(), SWT.NO_TRIM | SWT.ON_TOP);
            inlineRenameShell.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
            inlineRenameShell.setBackground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

            org.eclipse.swt.widgets.Label instructionLabel = new org.eclipse.swt.widgets.Label(inlineRenameShell, SWT.NONE);
            instructionLabel.setText("Enter new name, press Enter to rename:");
            instructionLabel.setBackground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            instructionLabel.setForeground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
            org.eclipse.swt.layout.GridData labelData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false);
            labelData.horizontalIndent = 4;
            labelData.verticalIndent = 2;
            instructionLabel.setLayoutData(labelData);
            
            inlineRenameText = new Text(inlineRenameShell, SWT.BORDER);
            inlineRenameText.setText(originalSymbolName);
            inlineRenameText.selectAll();
            
            org.eclipse.swt.layout.GridData textData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false);
            textData.horizontalIndent = 4;
            textData.verticalIndent = 2;
            textData.minimumWidth = Math.max(200, originalSymbolName.length() * 12 + 40); // larger minimum width
            textData.widthHint = textData.minimumWidth;
            inlineRenameText.setLayoutData(textData);
            
            Point shellSize = inlineRenameShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            inlineRenameShell.setSize(shellSize.x + 8, shellSize.y + 4); // add padding
            inlineRenameShell.setLocation(location.x, location.y);
            
            setupInlineRenameEventHandlers();
            
            inlineRenameShell.open();
            inlineRenameText.setFocus();
            
        } catch (Exception e) {
            System.out.println("[RebecaRename] createInlineRenameWidget failed: " + e.getMessage());
            e.printStackTrace();
            hideInlineRenameWidget();
        }
    }
    
    private Point getCursorScreenLocation(ISourceViewer sourceViewer) {
        System.out.println("[RebecaRename] getCursorScreenLocation started");
        try {
            org.eclipse.swt.custom.StyledText textWidget = sourceViewer.getTextWidget();
            if (textWidget == null) {
                return null;
            }
            
            int caretOffset = textWidget.getCaretOffset();
            
            Point location = textWidget.getLocationAtOffset(caretOffset);
            
            Point screenLocation = textWidget.toDisplay(location);
            return screenLocation;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void setupInlineRenameEventHandlers() {
        inlineRenameText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    performInlineRename();
                } else if (e.keyCode == SWT.ESC) {
                    hideInlineRenameWidget();
                }
            }
        });
        
        inlineRenameText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideInlineRenameWidget();
            }
        });
    }
    
    private void performInlineRename() {
        try {
            String newName = inlineRenameText.getText().trim();
            
            if (newName.isEmpty() || newName.equals(originalSymbolName)) {
                hideInlineRenameWidget();
                return;
            }
            
            if (!isValidName(newName)) {
                showError(inlineRenameShell, "Invalid identifier name: " + newName);
                return;
            }
            
            hideInlineRenameWidget();
            
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
            
            performRename(currentEditor.getSite().getShell(), newName, occurrences);
            
        } catch (Exception e) {
            hideInlineRenameWidget();
            showError(currentEditor.getSite().getShell(), "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void hideInlineRenameWidget() {
        if (inlineRenameShell != null && !inlineRenameShell.isDisposed()) {
            inlineRenameShell.dispose();
        }
        inlineRenameShell = null;
        inlineRenameText = null;
    }
    
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        name = name.trim();
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        
        return !isReservedKeyword(name);
    }
    
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
    
    private SymbolAnalysisResult analyzeSymbolAtPosition(IDocument document, int offset) {
        try {
            IRegion wordRegion = getWordRegion(document, offset);
            if (wordRegion == null) {
                return null;
            }
            
            String word = document.get(wordRegion.getOffset(), wordRegion.getLength());
            if (word.trim().isEmpty()) {
                return null;
            }
            
        RebecaRefactoringParticipant.SymbolType symbolType = determineSymbolType(document, wordRegion.getOffset(), word);
        if (symbolType == null) {
            return null;
        }
        
        SymbolAnalysisResult result = new SymbolAnalysisResult(word, symbolType);
        return result;
            
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("[RebecaRename] Exception in analyzeSymbolAtPosition: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private IRegion getWordRegion(IDocument document, int offset) throws BadLocationException {
        int start = offset;
        int end = offset;
        
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            start--;
        }
        
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
    
    private RebecaRefactoringParticipant.SymbolType determineSymbolType(IDocument document, int offset, String word) {
        try {
            String content = document.get();
            
            int lineStart = document.getLineOffset(document.getLineOfOffset(offset));
            int lineEnd = lineStart + document.getLineLength(document.getLineOfOffset(offset));
            String line = content.substring(lineStart, lineEnd);
            
            int positionInLine = offset - lineStart;
            
			java.util.regex.Pattern classDeclPattern = java.util.regex.Pattern.compile("\\breactiveclass\\s+(" + java.util.regex.Pattern.quote(word) + ")\\b");
			if (classDeclPattern.matcher(line).find()) {
				return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
			}

			java.util.regex.Pattern ctorDeclPattern = java.util.regex.Pattern.compile("^\\s*" + java.util.regex.Pattern.quote(word) + "\\s*\\(.*");
			if (ctorDeclPattern.matcher(line).find()) {
				if (content.contains("reactiveclass " + word)) {
					return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
				}
			}

            if (line.matches(".*\\bmsgsrv\\s+" + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            if (line.matches(".*\\w+\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*") ||
                line.matches(".*\\bself\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            String beforeWord = line.substring(0, positionInLine);
            String afterWord = line.substring(positionInLine + word.length());
            
            if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+\\w+.*")) {
                return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
            }
            
            if (isInSection(content, offset, "knownrebecs")) {
                // beforeWord should only contain whitespace (indentation), and afterWord should have variables
                if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+[\\w\\s,]+;.*")) {
                    // Pattern: ClassName varName1, varName2, ...;
                    return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
                } 
                else if (afterWord.matches("\\s*[,;].*") || afterWord.matches("\\s+\\w+.*[,;].*")) {
                    // Pattern: varName1, varName2, ... or varName;
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
                else if (beforeWord.matches(".*\\w\\s*$")) {
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                } else {
                    // default to instance name in knownrebecs
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
            }
            
            if (isInSection(content, offset, "main")) {
                // check if this is a class usage: ClassName instanceName(params):();
                if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+\\w+\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;.*")) {
                    // Pattern: ClassName instanceName(params):();
                    return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
                }
                // check if this is an instance name: ClassName instanceName(params):();
                else if (beforeWord.matches(".*\\w\\s+$") && afterWord.matches("\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;.*")) {
                    // pattern: instanceName after class name
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
                // check if it's a parameter reference inside parentheses
                else if (content.substring(0, offset).matches(".*\\([^)]*")) {
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
                
                // fallback for main section: any other identifier is likely an instance name
                return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
            }
            
            if (isInSection(content, offset, "statevars")) {
                return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            }
            
            if (line.matches("\\s*" + java.util.regex.Pattern.quote(word) + "\\s*=.*")) {
                return RebecaRefactoringParticipant.SymbolType.PROPERTY_NAME;
            }
            
            // default: treat as variable or instance name
            if (line.contains(".")) {
                // If it's part of object.field, likely an instance or variable
                if (beforeWord.endsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME; // field access
                } else if (afterWord.startsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME; // object access
                }
            }
            
            // default to variable name for standalone identifiers
            return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    private boolean isInSection(String content, int offset, String sectionName) {
        String beforeOffset = content.substring(0, offset);
        
        int lastSectionStart = beforeOffset.lastIndexOf(sectionName);
        if (lastSectionStart == -1) {
            return false;
        }
        
        int braceStart = content.indexOf('{', lastSectionStart);
        if (braceStart == -1 || braceStart > offset) {
            return false;
        }
        
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

    private void performRename(Shell shell, String newName, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences) {
        try {
            java.util.Map<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> fileOccurrences = new java.util.HashMap<>();
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                fileOccurrences.computeIfAbsent(occ.file, k -> new java.util.ArrayList<>()).add(occ);
            }
            
            int totalRenamed = 0;
            for (java.util.Map.Entry<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> entry : fileOccurrences.entrySet()) {
                totalRenamed += renameInFile(entry.getKey(), entry.getValue(), newName);
            }
            
            MessageDialog.openInformation(shell, "Rename Complete", 
                String.format("Successfully renamed %d occurrence(s) in %d file(s).", 
                            totalRenamed, fileOccurrences.size()));
            
        } catch (Exception e) {
            showError(shell, "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int renameInFile(IFile file, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences, String newName) throws Exception {
        org.eclipse.ui.texteditor.IDocumentProvider provider = new org.eclipse.ui.editors.text.TextFileDocumentProvider();
        provider.connect(file);
        
        try {
            IDocument document = provider.getDocument(file);
            
            occurrences.sort((a, b) -> Integer.compare(b.offset, a.offset));
            
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                document.replace(occ.offset, occ.length, newName);
            }
            provider.saveDocument(null, file, document, true);
            
            return occurrences.size();
            
        } finally {
            provider.disconnect(file);
        }
    }
    
    private void showError(Shell shell, String message) {
        MessageDialog.openError(shell, "Rename Error", message);
    }
    
    private static class SymbolAnalysisResult {
        final String symbolName;
        final RebecaRefactoringParticipant.SymbolType symbolType;
        
        SymbolAnalysisResult(String symbolName, RebecaRefactoringParticipant.SymbolType symbolType) {
            this.symbolName = symbolName;
            this.symbolType = symbolType;
        }
    }
}
