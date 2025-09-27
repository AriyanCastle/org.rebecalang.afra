package org.rebecalang.afra.ideplugin.refactoring;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RebecaRenameDialog extends Dialog {
    
    private String originalName;
    private String newName;
    private RebecaRefactoringParticipant.SymbolType symbolType;
    private List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences;
    private Text newNameText;
    private CheckboxTableViewer occurrenceViewer;
    private List<RebecaRefactoringParticipant.SymbolOccurrence> selectedOccurrences;
    
    public RebecaRenameDialog(Shell parentShell, String originalName, RebecaRefactoringParticipant.SymbolType symbolType, 
                             List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences) {
        super(parentShell);
        this.originalName = originalName;
        this.newName = originalName;
        this.symbolType = symbolType;
        this.occurrences = occurrences;
        this.selectedOccurrences = occurrences;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Rename " + getSymbolTypeDisplayName());
        shell.setSize(600, 500);
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));
        
        Label originalLabel = new Label(container, SWT.NONE);
        originalLabel.setText("Current name:");
        originalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        Label originalNameLabel = new Label(container, SWT.NONE);
        originalNameLabel.setText(originalName);
        originalNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        originalNameLabel.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));
        
        Label newLabel = new Label(container, SWT.NONE);
        newLabel.setText("New name:");
        newLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        newNameText = new Text(container, SWT.BORDER);
        newNameText.setText(originalName);
        newNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        newNameText.selectAll();
        newNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                newName = newNameText.getText();
                validateInput();
            }
        });
        
        Label occurrencesLabel = new Label(container, SWT.NONE);
        occurrencesLabel.setText("Occurrences to rename:");
        GridData occurrencesLabelData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        occurrencesLabelData.horizontalSpan = 2;
        occurrencesLabelData.verticalIndent = 10;
        occurrencesLabel.setLayoutData(occurrencesLabelData);
        
        occurrenceViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.horizontalSpan = 2;
        tableData.heightHint = 200;
        occurrenceViewer.getControl().setLayoutData(tableData);
        
        occurrenceViewer.setContentProvider(ArrayContentProvider.getInstance());
        occurrenceViewer.setLabelProvider(new OccurrenceLabelProvider());
        occurrenceViewer.setInput(occurrences);
        occurrenceViewer.setAllChecked(true);
        
        Label summaryLabel = new Label(container, SWT.NONE);
        summaryLabel.setText(String.format("Found %d occurrence(s) of '%s' in %d file(s)", 
                                          occurrences.size(), originalName, getUniqueFileCount()));
        GridData summaryData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        summaryData.horizontalSpan = 2;
        summaryData.verticalIndent = 5;
        summaryLabel.setLayoutData(summaryData);
        
        return container;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Rename", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        
        validateInput();
    }
    
    @Override
    protected void okPressed() {
        Object[] checkedElements = occurrenceViewer.getCheckedElements();
        selectedOccurrences = new java.util.ArrayList<>();
        for (Object element : checkedElements) {
            selectedOccurrences.add((RebecaRefactoringParticipant.SymbolOccurrence) element);
        }
        
        super.okPressed();
    }
    
    private void validateInput() {
        boolean valid = isValidName(newName) && !newName.equals(originalName);
        getButton(IDialogConstants.OK_ID).setEnabled(valid);
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
    
    private String getSymbolTypeDisplayName() {
        switch (symbolType) {
            case CLASS_NAME: return "Class";
            case METHOD_NAME: return "Method";
            case VARIABLE_NAME: return "Variable";
            case INSTANCE_NAME: return "Instance";
            case PROPERTY_NAME: return "Property";
            default: return "Symbol";
        }
    }
    
    private int getUniqueFileCount() {
        return (int) occurrences.stream()
                .map(occ -> occ.file)
                .distinct()
                .count();
    }
    
    public String getNewName() {
        return newName;
    }
    
    public List<RebecaRefactoringParticipant.SymbolOccurrence> getSelectedOccurrences() {
        return selectedOccurrences;
    }
    

    private class OccurrenceLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof RebecaRefactoringParticipant.SymbolOccurrence) {
                RebecaRefactoringParticipant.SymbolOccurrence occ = (RebecaRefactoringParticipant.SymbolOccurrence) element;
                String contextInfo = "";
                
                if (occ.context.isDeclaration) {
                    contextInfo = " (declaration)";
                } else {
                    contextInfo = " (usage)";
                }
                
                return String.format("%s:%d - %s%s", 
                                   occ.file.getName(), 
                                   getLineNumber(occ),
                                   occ.originalName,
                                   contextInfo);
            }
            return super.getText(element);
        }
        
        private int getLineNumber(RebecaRefactoringParticipant.SymbolOccurrence occ) {
            try {
                org.eclipse.ui.texteditor.IDocumentProvider provider = new org.eclipse.ui.editors.text.TextFileDocumentProvider();
                provider.connect(occ.file);
                org.eclipse.jface.text.IDocument document = provider.getDocument(occ.file);
                int lineNumber = document.getLineOfOffset(occ.offset) + 1;
                provider.disconnect(occ.file);
                return lineNumber;
            } catch (Exception e) {
                return 1;
            }
        }
    }
}
