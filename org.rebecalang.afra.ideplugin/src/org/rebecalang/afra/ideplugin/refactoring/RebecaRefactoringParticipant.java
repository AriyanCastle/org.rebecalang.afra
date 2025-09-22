package org.rebecalang.afra.ideplugin.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Main refactoring engine for Rebeca files that provides semantic-aware renaming
 */
public class RebecaRefactoringParticipant {
    
    /**
     * Types of symbols that can be refactored
     */
    public enum SymbolType {
        CLASS_NAME,
        METHOD_NAME,
        VARIABLE_NAME,
        INSTANCE_NAME,
        PROPERTY_NAME
    }
    
    /**
     * Represents a symbol occurrence in the code
     */
    public static class SymbolOccurrence {
        public final IFile file;
        public final int offset;
        public final int length;
        public final String originalName;
        public final SymbolType type;
        public final SymbolContext context;
        
        public SymbolOccurrence(IFile file, int offset, int length, String originalName, 
                               SymbolType type, SymbolContext context) {
            this.file = file;
            this.offset = offset;
            this.length = length;
            this.originalName = originalName;
            this.type = type;
            this.context = context;
        }
    }
    
    /**
     * Context information for symbol analysis
     */
    public static class SymbolContext {
        public final String className;
        public final String methodName;
        public final boolean isDeclaration;
        
        public SymbolContext(String className, String methodName, boolean isDeclaration) {
            this.className = className;
            this.methodName = methodName;
            this.isDeclaration = isDeclaration;
        }
    }
    
    private final IProject project;
    
    public RebecaRefactoringParticipant(IProject project) {
        this.project = project;
    }
    
    /**
     * Find all occurrences of a symbol in the current file and its corresponding paired file
     */
    public List<SymbolOccurrence> findAllOccurrences(String symbolName, SymbolType symbolType, 
                                                    IFile originFile, int originOffset) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        try {
            // Get the current file and its corresponding paired file
            List<IFile> pairedFiles = findPairedFiles(originFile);
            
            for (IFile file : pairedFiles) {
                occurrences.addAll(findOccurrencesInFile(file, symbolName, symbolType, originFile, originOffset));
            }
            
        } catch (Exception e) {
            System.err.println("Error finding symbol occurrences: " + e.getMessage());
        }
        
        return occurrences;
    }
    
    /**
     * Find the current file and its corresponding paired file (same base name, different extension)
     */
    private List<IFile> findPairedFiles(IFile originFile) {
        List<IFile> pairedFiles = new ArrayList<>();
        
        // Always include the origin file
        pairedFiles.add(originFile);
        
        // Get the base name (filename without extension)
        String fileName = originFile.getName();
        String extension = originFile.getFileExtension();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // Determine the paired extension
        String pairedExtension;
        if ("rebeca".equals(extension)) {
            pairedExtension = "property";
        } else if ("property".equals(extension)) {
            pairedExtension = "rebeca";
        } else {
            // Unknown extension, return only the origin file
            return pairedFiles;
        }
        
        // Look for the paired file in the same folder
        try {
            IContainer parent = originFile.getParent();
            String pairedFileName = baseName + "." + pairedExtension;
            IFile pairedFile = parent.getFile(new org.eclipse.core.runtime.Path(pairedFileName));
            
            // Only add if the paired file exists
            if (pairedFile.exists()) {
                pairedFiles.add(pairedFile);
            }
        } catch (Exception e) {
            System.err.println("Error finding paired file: " + e.getMessage());
        }
        
        return pairedFiles;
    }
    
    /**
     * Find all Rebeca and property files in the project
     */
    private List<IFile> findRebecaFiles(IProject project) throws CoreException {
        List<IFile> files = new ArrayList<>();
        findRebecaFilesRecursive(project, files);
        return files;
    }
    
    private void findRebecaFilesRecursive(IResource resource, List<IFile> files) throws CoreException {
        if (resource.getType() == IResource.FILE) {
            IFile file = (IFile) resource;
            String extension = file.getFileExtension();
            if ("rebeca".equals(extension) || "property".equals(extension)) {
                files.add(file);
            }
        } else if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
            for (IResource child : ((org.eclipse.core.resources.IContainer) resource).members()) {
                findRebecaFilesRecursive(child, files);
            }
        }
    }
    
    /**
     * Find occurrences of a symbol in a specific file
     */
    private List<SymbolOccurrence> findOccurrencesInFile(IFile file, String symbolName, 
                                                        SymbolType symbolType, IFile originFile, int originOffset) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        try {
            IDocumentProvider provider = new TextFileDocumentProvider();
            provider.connect(file);
            IDocument document = provider.getDocument(file);
            
            if (document != null) {
                String content = document.get();
                String extension = file.getFileExtension();
                
                if ("rebeca".equals(extension)) {
                    occurrences.addAll(findOccurrencesInRebecaFile(file, content, symbolName, symbolType));
                } else if ("property".equals(extension)) {
                    occurrences.addAll(findOccurrencesInPropertyFile(file, content, symbolName, symbolType));
                }
            }
            
            provider.disconnect(file);
            
        } catch (Exception e) {
            System.err.println("Error analyzing file " + file.getName() + ": " + e.getMessage());
        }
        
        return occurrences;
    }
    
    /**
     * Find symbol occurrences in a Rebeca file
     */
    private List<SymbolOccurrence> findOccurrencesInRebecaFile(IFile file, String content, 
                                                              String symbolName, SymbolType symbolType) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        switch (symbolType) {
            case CLASS_NAME:
                occurrences.addAll(findClassNameOccurrences(file, content, symbolName));
                break;
            case METHOD_NAME:
                occurrences.addAll(findMethodNameOccurrences(file, content, symbolName));
                break;
            case VARIABLE_NAME:
                occurrences.addAll(findVariableNameOccurrences(file, content, symbolName));
                break;
            case INSTANCE_NAME:
                occurrences.addAll(findInstanceNameOccurrences(file, content, symbolName));
                break;
        }
        
        return occurrences;
    }
    
    /**
     * Find class name occurrences
     */
    private List<SymbolOccurrence> findClassNameOccurrences(IFile file, String content, String className) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for class declaration: reactiveclass ClassName
        Pattern classDeclarationPattern = Pattern.compile("\\breactiveclass\\s+(" + Pattern.quote(className) + ")\\b");
        Matcher matcher = classDeclarationPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, 
                                               SymbolType.CLASS_NAME, new SymbolContext(className, null, true)));
        }
        
        // Pattern for class usage in main block: ClassName identifier
        Pattern classUsagePattern = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*\\([^)]*\\)\\s*;");
        matcher = classUsagePattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, 
                                               SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
        }
        
        // Pattern for class usage in knownrebecs: ClassName varName1, varName2, ...;
        Pattern knownrebecPattern = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\s+[\\w\\s,]+;");
        matcher = knownrebecPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, 
                                               SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
        }
        
        return occurrences;
    }
    
    /**
     * Find method name occurrences
     */
    private List<SymbolOccurrence> findMethodNameOccurrences(IFile file, String content, String methodName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for method declaration: msgsrv methodName
        Pattern methodDeclarationPattern = Pattern.compile("\\bmsgsrv\\s+(" + Pattern.quote(methodName) + ")\\s*\\(");
        Matcher matcher = methodDeclarationPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, 
                                               SymbolType.METHOD_NAME, new SymbolContext(null, methodName, true)));
        }
        
        // Pattern for method calls: identifier.methodName() or self.methodName()
        Pattern methodCallPattern = Pattern.compile("\\b(\\w+|self)\\.(" + Pattern.quote(methodName) + ")\\s*\\(");
        matcher = methodCallPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(2);
            occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, 
                                               SymbolType.METHOD_NAME, new SymbolContext(null, methodName, false)));
        }
        
        return occurrences;
    }
    
    /**
     * Find variable name occurrences
     */
    private List<SymbolOccurrence> findVariableNameOccurrences(IFile file, String content, String variableName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for variable declarations in statevars
        Pattern statevarPattern = Pattern.compile("\\b\\w+\\s+([^;,=]+)");
        Matcher statevarMatcher = statevarPattern.matcher(getStatevarsSection(content));
        while (statevarMatcher.find()) {
            String vars = statevarMatcher.group(1);
            String[] varNames = vars.split("\\s*,\\s*");
            for (String varName : varNames) {
                varName = varName.trim();
                if (varName.equals(variableName)) {
                    // Find offset in original content
                    int relativeOffset = statevarMatcher.start(1) + vars.indexOf(varName);
                    int absoluteOffset = findStatevarsOffset(content) + relativeOffset;
                    occurrences.add(new SymbolOccurrence(file, absoluteOffset, variableName.length(), variableName, 
                                                       SymbolType.VARIABLE_NAME, new SymbolContext(null, null, true)));
                }
            }
        }
        
        // Pattern for variable usage: standalone variable references
        Pattern variableUsagePattern = Pattern.compile("\\b(" + Pattern.quote(variableName) + ")\\b");
        Matcher matcher = variableUsagePattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            // Skip if it's in a declaration context or part of another identifier
            if (!isInDeclarationContext(content, offset) && !isPartOfLargerIdentifier(content, offset, variableName)) {
                occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName, 
                                                   SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
            }
        }
        
        return occurrences;
    }
    
    /**
     * Find instance name occurrences
     */
    private List<SymbolOccurrence> findInstanceNameOccurrences(IFile file, String content, String instanceName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for instance declarations in knownrebecs
        Pattern knownrebecPattern = Pattern.compile("\\w+\\s+([^;,]+)");
        Matcher knownrebecMatcher = knownrebecPattern.matcher(getKnownrebecssection(content));
        while (knownrebecMatcher.find()) {
            String instances = knownrebecMatcher.group(1);
            String[] instanceNames = instances.split("\\s*,\\s*");
            for (String instName : instanceNames) {
                instName = instName.trim();
                if (instName.equals(instanceName)) {
                    // Find offset in original content
                    int relativeOffset = knownrebecMatcher.start(1) + instances.indexOf(instName);
                    int absoluteOffset = findKnownrebecsOffset(content) + relativeOffset;
                    occurrences.add(new SymbolOccurrence(file, absoluteOffset, instanceName.length(), instanceName, 
                                                       SymbolType.INSTANCE_NAME, new SymbolContext(null, null, true)));
                }
            }
        }
        
        // Pattern for instance usage: instanceName.method() or standalone instanceName
        Pattern instanceUsagePattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")(?:\\.|\\b)");
        Matcher matcher = instanceUsagePattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            if (!isInDeclarationContext(content, offset)) {
                occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName, 
                                                   SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
            }
        }
        
        return occurrences;
    }
    
    /**
     * Find symbol occurrences in a property file
     */
    private List<SymbolOccurrence> findOccurrencesInPropertyFile(IFile file, String content, 
                                                                String symbolName, SymbolType symbolType) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        switch (symbolType) {
            case PROPERTY_NAME:
                occurrences.addAll(findPropertyNameOccurrences(file, content, symbolName));
                break;
            case INSTANCE_NAME:
                occurrences.addAll(findInstanceReferencesInProperty(file, content, symbolName));
                break;
            case VARIABLE_NAME:
                occurrences.addAll(findVariableReferencesInProperty(file, content, symbolName));
                break;
        }
        
        return occurrences;
    }
    
    /**
     * Find property name occurrences in property files
     */
    private List<SymbolOccurrence> findPropertyNameOccurrences(IFile file, String content, String propertyName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for property definition: propertyName = expression
        Pattern propertyDefPattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\s*=");
        Matcher matcher = propertyDefPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName, 
                                               SymbolType.PROPERTY_NAME, new SymbolContext(null, null, true)));
        }
        
        // Pattern for property usage: standalone propertyName
        Pattern propertyUsagePattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\b");
        matcher = propertyUsagePattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            if (!isInPropertyDefinition(content, offset)) {
                occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName, 
                                                   SymbolType.PROPERTY_NAME, new SymbolContext(null, null, false)));
            }
        }
        
        return occurrences;
    }
    
    /**
     * Find instance references in property files (instance.field)
     */
    private List<SymbolOccurrence> findInstanceReferencesInProperty(IFile file, String content, String instanceName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for instance.field references
        Pattern instanceRefPattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")\\.");
        Matcher matcher = instanceRefPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName, 
                                               SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
        }
        
        return occurrences;
    }
    
    /**
     * Find variable references in property files (instance.variableName)
     */
    private List<SymbolOccurrence> findVariableReferencesInProperty(IFile file, String content, String variableName) {
        List<SymbolOccurrence> occurrences = new ArrayList<>();
        
        // Pattern for instance.variableName references
        Pattern variableRefPattern = Pattern.compile("\\w+\\.(" + Pattern.quote(variableName) + ")\\b");
        Matcher matcher = variableRefPattern.matcher(content);
        while (matcher.find()) {
            int offset = matcher.start(1);
            occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName, 
                                               SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
        }
        
        return occurrences;
    }
    
    // Helper methods for context analysis
    
    private String getStatevarsSection(String content) {
        Pattern pattern = Pattern.compile("statevars\\s*\\{([^}]*)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }
    
    private String getKnownrebecssection(String content) {
        Pattern pattern = Pattern.compile("knownrebecs\\s*\\{([^}]*)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }
    
    private int findStatevarsOffset(String content) {
        Pattern pattern = Pattern.compile("statevars\\s*\\{");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.end() : 0;
    }
    
    private int findKnownrebecsOffset(String content) {
        Pattern pattern = Pattern.compile("knownrebecs\\s*\\{");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.end() : 0;
    }
    
    private boolean isInDeclarationContext(String content, int offset) {
        // Check if the offset is within a declaration block
        String beforeOffset = content.substring(0, offset);
        
        // Check for statevars or knownrebecs context
        if (beforeOffset.lastIndexOf("statevars") > beforeOffset.lastIndexOf("}") ||
            beforeOffset.lastIndexOf("knownrebecs") > beforeOffset.lastIndexOf("}")) {
            return true;
        }
        
        return false;
    }
    
    private boolean isPartOfLargerIdentifier(String content, int offset, String name) {
        // Check if this name is part of a larger identifier
        if (offset > 0 && Character.isJavaIdentifierPart(content.charAt(offset - 1))) {
            return true;
        }
        if (offset + name.length() < content.length() && 
            Character.isJavaIdentifierPart(content.charAt(offset + name.length()))) {
            return true;
        }
        return false;
    }
    
    private boolean isInPropertyDefinition(String content, int offset) {
        // Check if this is in a property definition (before =)
        String lineStart = content.substring(0, offset);
        int lastNewline = lineStart.lastIndexOf('\n');
        String currentLine = lineStart.substring(lastNewline + 1);
        return !currentLine.contains("=");
    }
}
