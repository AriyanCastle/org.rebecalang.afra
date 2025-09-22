package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.rebecalang.compiler.utils.CodeCompilationException;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.ExceptionContainer;
import org.rebecalang.rmc.FileGeneratorProperties;
import org.rebecalang.rmc.ModelCheckersFilesGenerator;
import org.rebecalang.rmc.RMCConfig;
import org.rebecalang.rmc.timedrebeca.TimedRebecaFileGeneratorProperties;
import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.TimedRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Real-time syntax checker for Property files with debouncing
 * Handles dependency on corresponding .rebeca files
 */
public class PropertyRealTimeSyntaxChecker implements IDocumentListener {
    
    private static final int DEBOUNCE_DELAY_MS = 600;
    
    @Autowired
    private ExceptionContainer exceptionContainer;
    
    @Autowired
    private ModelCheckersFilesGenerator modelCheckersFilesGenerator;
    
    private final RebecaPropEditor editor;
    private final IFile propertyFile;
    private Timer debounceTimer;
    private volatile boolean isChecking = false;
    
    public PropertyRealTimeSyntaxChecker(RebecaPropEditor editor, IFile propertyFile) {
        this.editor = editor;
        this.propertyFile = propertyFile;
        
        // Initialize Spring context for compiler components
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Initializing compiler components...");
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
            System.out.println("PropertyRealTimeSyntaxChecker: Compiler components initialized successfully");
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to initialize compiler components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        // Cancel any pending syntax check
        System.out.println("PropertyRealTimeSyntaxChecker: Document about to be changed");
        cancelPendingCheck();
    }
    
    @Override
    public void documentChanged(DocumentEvent event) {
        // Schedule a new syntax check with debouncing
        System.out.println("PropertyRealTimeSyntaxChecker: Document changed, scheduling syntax check");
        scheduleDelayedSyntaxCheck();
    }
    
    private void cancelPendingCheck() {
        if (debounceTimer != null) {
            debounceTimer.cancel();
            debounceTimer = null;
        }
    }
    
    private void scheduleDelayedSyntaxCheck() {
        cancelPendingCheck();
        
        System.out.println("PropertyRealTimeSyntaxChecker: Scheduling syntax check with " + DEBOUNCE_DELAY_MS + "ms delay");
        debounceTimer = new Timer(true); // Daemon timer
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("PropertyRealTimeSyntaxChecker: Timer fired, performing syntax check");
                performSyntaxCheck();
            }
        }, DEBOUNCE_DELAY_MS);
    }
    
    private void performSyntaxCheck() {
        if (isChecking) {
            System.out.println("PropertyRealTimeSyntaxChecker: Already checking, skipping");
            return; // Avoid concurrent checks
        }
        
        try {
            isChecking = true;
            System.out.println("PropertyRealTimeSyntaxChecker: Starting syntax check");
            
            // Run syntax check in UI thread for marker operations
            Display.getDefault().asyncExec(() -> {
                try {
                    doSyntaxCheck();
                } finally {
                    isChecking = false;
                    System.out.println("PropertyRealTimeSyntaxChecker: Syntax check completed");
                }
            });
            
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Error in syntax check: " + e.getMessage());
            e.printStackTrace();
            isChecking = false;
        }
    }
    
    private void doSyntaxCheck() {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Starting doSyntaxCheck()");
            
            // Clear existing syntax error markers
            clearSyntaxMarkers();
            
            // Get corresponding rebeca file
            File rebecaFile = getCorrespondingRebecaFile();
            if (rebecaFile == null || !rebecaFile.exists()) {
                System.out.println("PropertyRealTimeSyntaxChecker: Corresponding rebeca file not found or doesn't exist");
                createMissingRebecaFileMarker();
                return;
            }
            
            // Get current property document content
            IDocument document = editor.getPublicSourceViewer().getDocument();
            String content = document.get();
            
            System.out.println("PropertyRealTimeSyntaxChecker: Property content length: " + content.length());
            System.out.println("PropertyRealTimeSyntaxChecker: Rebeca file: " + rebecaFile.getAbsolutePath());
            
            // Save property content to temporary file for syntax checking
            File tempPropertyFile = createTempFileWithContent(content, ".property");
            System.out.println("PropertyRealTimeSyntaxChecker: Created temp property file: " + tempPropertyFile.getAbsolutePath());
            
            try {
                // Perform syntax-only compilation with both files
                checkSyntaxOnly(rebecaFile, tempPropertyFile);
                
            } finally {
                // Clean up temporary file
                if (tempPropertyFile != null && tempPropertyFile.exists()) {
                    tempPropertyFile.delete();
                    System.out.println("PropertyRealTimeSyntaxChecker: Cleaned up temp property file");
                }
            }
            
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Syntax check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private File getCorrespondingRebecaFile() {
        try {
            // Use the same logic as CompilationAndCodeGenerationProcess
            return CompilationAndCodeGenerationProcess.getRebecaFileFromPropertyFile(propertyFile);
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Error getting corresponding rebeca file: " + e.getMessage());
            return null;
        }
    }
    
    private void clearSyntaxMarkers() {
        try {
            // Remove only syntax error markers (keep other markers intact)
            IMarker[] markers = propertyFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
            for (IMarker marker : markers) {
                String source = marker.getAttribute("syntaxChecker", null);
                if ("realTimeProperty".equals(source)) {
                    marker.delete();
                }
            }
        } catch (CoreException e) {
            System.err.println("Failed to clear syntax markers: " + e.getMessage());
        }
    }
    
    private File createTempFileWithContent(String content, String extension) throws IOException {
        File tempFile = File.createTempFile("property_syntax_", extension);
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write(content);
        }
        
        // Debug: Read back the file to verify content
        try {
            String readBack = java.nio.file.Files.readString(tempFile.toPath());
            System.out.println("PropertyRealTimeSyntaxChecker: Temp file content length: " + readBack.length());
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to read back temp file: " + e.getMessage());
        }
        
        return tempFile;
    }
    
    private void checkSyntaxOnly(File rebecaFile, File tempPropertyFile) {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Starting checkSyntaxOnly()");
            
            // Check if components are initialized
            if (exceptionContainer == null) {
                System.err.println("PropertyRealTimeSyntaxChecker: exceptionContainer is null!");
                return;
            }
            if (modelCheckersFilesGenerator == null) {
                System.err.println("PropertyRealTimeSyntaxChecker: modelCheckersFilesGenerator is null!");
                return;
            }
            
            // Clear exception container
            exceptionContainer.clear();
            System.out.println("PropertyRealTimeSyntaxChecker: Cleared exception container");
            
            // Get compiler extensions (same as full compilation)
            Set<CompilerExtension> extensions = getCompilerExtensions();
            System.out.println("PropertyRealTimeSyntaxChecker: Got extensions: " + extensions.size());
            
            // Create minimal file generator properties for syntax checking only
            FileGeneratorProperties fileGeneratorProperties = createMinimalFileGeneratorProperties();
            System.out.println("PropertyRealTimeSyntaxChecker: Created file generator properties");
            
            // Create a temporary output directory that we'll ignore
            File tempOutputDir = File.createTempFile("property_syntax_", "_tmp");
            tempOutputDir.delete();
            tempOutputDir.mkdirs();
            System.out.println("PropertyRealTimeSyntaxChecker: Created temp output dir: " + tempOutputDir.getAbsolutePath());
            
            try {
                System.out.println("PropertyRealTimeSyntaxChecker: Calling modelCheckersFilesGenerator.generateFiles()");
                System.out.println("PropertyRealTimeSyntaxChecker: - rebecaFile: " + rebecaFile.getAbsolutePath());
                System.out.println("PropertyRealTimeSyntaxChecker: - tempPropertyFile: " + tempPropertyFile.getAbsolutePath());
                System.out.println("PropertyRealTimeSyntaxChecker: - tempOutputDir: " + tempOutputDir.getAbsolutePath());
                System.out.println("PropertyRealTimeSyntaxChecker: - extensions: " + extensions);
                
                // Key difference: pass both rebeca file AND property file to compiler
                modelCheckersFilesGenerator.generateFiles(
                    rebecaFile,                // Original rebeca file (required for property dependencies)
                    tempPropertyFile,          // Temporary property file with current content
                    tempOutputDir,             // Minimal temp directory
                    extensions,
                    fileGeneratorProperties
                );
                System.out.println("PropertyRealTimeSyntaxChecker: generateFiles() completed successfully");
            } finally {
                // Clean up temp directory
                if (tempOutputDir.exists()) {
                    deleteDirectory(tempOutputDir);
                    System.out.println("PropertyRealTimeSyntaxChecker: Cleaned up temp output dir");
                }
            }
            
            // Check for syntax errors and create markers (property file specific)
            System.out.println("PropertyRealTimeSyntaxChecker: Checking for exceptions. Empty? " + exceptionContainer.exceptionsIsEmpty());
            if (!exceptionContainer.exceptionsIsEmpty()) {
                System.out.println("PropertyRealTimeSyntaxChecker: Found exceptions, creating error markers");
                createSyntaxErrorMarkers(tempPropertyFile);
            } else {
                System.out.println("PropertyRealTimeSyntaxChecker: No syntax errors found");
            }
            
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Syntax check compilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Set<CompilerExtension> getCompilerExtensions() {
        // Use the same extension retrieval logic as the main compiler
        try {
            Set<CompilerExtension> extensions = CompilationAndCodeGenerationProcess
                .retrieveCompationExtension(propertyFile.getProject());
            System.out.println("PropertyRealTimeSyntaxChecker: Retrieved extensions from project: " + extensions);
            
            // Empty extensions set is valid for core Rebeca - don't add anything
            if (extensions.isEmpty()) {
                System.out.println("PropertyRealTimeSyntaxChecker: Extensions empty, using core Rebeca (no extensions needed)");
            }
            
            return extensions;
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to get extensions: " + e.getMessage());
            // Fallback to empty extensions set (core Rebeca)
            Set<CompilerExtension> extensions = new java.util.HashSet<>();
            System.out.println("PropertyRealTimeSyntaxChecker: Using fallback empty extensions (core Rebeca)");
            return extensions;
        }
    }
    
    private FileGeneratorProperties createMinimalFileGeneratorProperties() {
        // Create properties using the same logic as the main compiler but optimized for syntax checking
        try {
            FileGeneratorProperties fileGeneratorProperties = null;
            String languageType = CoreRebecaProjectPropertyPage.getProjectType(propertyFile.getProject());
            
            switch (languageType) {
            case "ProbabilisitcTimedRebeca":
            case "TimedRebeca":
                TimedRebecaFileGeneratorProperties timedProps = new TimedRebecaFileGeneratorProperties();
                if (TimedRebecaProjectPropertyPage.getProjectSemanticsModelIsTTS(propertyFile.getProject())) {
                    timedProps.setTTS(true);
                }
                fileGeneratorProperties = timedProps;
                break;
            default:
                fileGeneratorProperties = new FileGeneratorProperties();
            }
            
            // Set core version
            CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(propertyFile.getProject());
            fileGeneratorProperties.setCoreVersion(version);
            
            // Optimizations for syntax checking only
            fileGeneratorProperties.setSafeMode(false); // Skip safe mode for speed
            fileGeneratorProperties.setProgressReport(false); // No progress needed
            // Don't set export state space for syntax checking
            
            return fileGeneratorProperties;
            
        } catch (Exception e) {
            // Fallback to minimal properties
            return new FileGeneratorProperties();
        }
    }
    
    private void createSyntaxErrorMarkers(File tempPropertyFile) {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Creating syntax error markers");
            
            // Debug: Print all files in exception container
            System.out.println("PropertyRealTimeSyntaxChecker: All files in exception container:");
            for (File f : exceptionContainer.getExceptions().keySet()) {
                System.out.println("PropertyRealTimeSyntaxChecker: - " + f.getAbsolutePath());
            }
            
            // Get exceptions for the temporary property file
            Set<Exception> exceptions = exceptionContainer.getExceptions().get(tempPropertyFile);
            System.out.println("PropertyRealTimeSyntaxChecker: Exceptions for temp property file (" + tempPropertyFile.getAbsolutePath() + "): " + (exceptions != null ? exceptions.size() : "null"));
            
            if (exceptions != null) {
                // Create marker for first error only (optimization)
                for (Exception exception : exceptions) {
                    System.out.println("PropertyRealTimeSyntaxChecker: Processing exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                    if (exception instanceof CodeCompilationException) {
                        CodeCompilationException cce = (CodeCompilationException) exception;
                        System.out.println("PropertyRealTimeSyntaxChecker: Creating marker for CodeCompilationException at line " + cce.getLine());
                        createSyntaxErrorMarker(cce);
                        break; // Stop at first error for performance
                    }
                }
            } else {
                System.out.println("PropertyRealTimeSyntaxChecker: No exceptions found for property file");
            }
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to create syntax error markers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createSyntaxErrorMarker(CodeCompilationException cce) {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Creating IMarker.PROBLEM for: " + cce.getMessage());
            IMarker marker = propertyFile.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
            marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
            marker.setAttribute("syntaxChecker", "realTimeProperty"); // Tag for identification
            System.out.println("PropertyRealTimeSyntaxChecker: Successfully created error marker at line " + cce.getLine());
        } catch (CoreException e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to create syntax error marker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createMissingRebecaFileMarker() {
        try {
            IMarker marker = propertyFile.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, "Corresponding .rebeca file not found. Property files require a .rebeca file with the same name.");
            marker.setAttribute(IMarker.LINE_NUMBER, 1);
            marker.setAttribute("syntaxChecker", "realTimeProperty");
            System.out.println("PropertyRealTimeSyntaxChecker: Created missing rebeca file marker");
        } catch (CoreException e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to create missing rebeca file marker: " + e.getMessage());
        }
    }
    
    /**
     * Start real-time syntax checking for this document
     */
    public void startChecking(IDocument document) {
        System.out.println("PropertyRealTimeSyntaxChecker: Starting checking for document");
        document.addDocumentListener(this);
        System.out.println("PropertyRealTimeSyntaxChecker: Document listener added");
        
        // Perform initial syntax check
        System.out.println("PropertyRealTimeSyntaxChecker: Scheduling initial syntax check");
        scheduleDelayedSyntaxCheck();
    }
    
    /**
     * Stop real-time syntax checking for this document
     */
    public void stopChecking(IDocument document) {
        document.removeDocumentListener(this);
        cancelPendingCheck();
        clearSyntaxMarkers();
    }
    
    /**
     * Utility method to recursively delete a directory
     */
    private void deleteDirectory(File directory) {
        try {
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        deleteDirectory(file);
                    }
                }
            }
            directory.delete();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Dispose of resources
     */
    public void dispose() {
        cancelPendingCheck();
        isChecking = false;
    }
}
