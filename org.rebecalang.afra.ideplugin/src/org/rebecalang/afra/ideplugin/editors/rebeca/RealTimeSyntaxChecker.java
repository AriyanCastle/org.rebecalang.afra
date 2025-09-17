package org.rebecalang.afra.ideplugin.editors.rebeca;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Real-time syntax checker for Rebeca files with debouncing
 */
public class RealTimeSyntaxChecker implements IDocumentListener {
    
    private static final int DEBOUNCE_DELAY_MS = 300;
    
    @Autowired
    private ExceptionContainer exceptionContainer;
    
    @Autowired
    private ModelCheckersFilesGenerator modelCheckersFilesGenerator;
    
    private final RebecaEditor editor;
    private final IFile file;
    private Timer debounceTimer;
    private volatile boolean isChecking = false;
    
    public RealTimeSyntaxChecker(RebecaEditor editor, IFile file) {
        this.editor = editor;
        this.file = file;
        
        // Initialize Spring context for compiler components
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            System.out.println("RealTimeSyntaxChecker: Initializing compiler components...");
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
            System.out.println("RealTimeSyntaxChecker: Compiler components initialized successfully");
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Failed to initialize compiler components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        // Cancel any pending syntax check
        System.out.println("RealTimeSyntaxChecker: Document about to be changed");
        cancelPendingCheck();
    }
    
    @Override
    public void documentChanged(DocumentEvent event) {
        // Schedule a new syntax check with debouncing
        System.out.println("RealTimeSyntaxChecker: Document changed, scheduling syntax check");
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
        
        System.out.println("RealTimeSyntaxChecker: Scheduling syntax check with " + DEBOUNCE_DELAY_MS + "ms delay");
        debounceTimer = new Timer(true); // Daemon timer
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("RealTimeSyntaxChecker: Timer fired, performing syntax check");
                performSyntaxCheck();
            }
        }, DEBOUNCE_DELAY_MS);
    }
    
    private void performSyntaxCheck() {
        if (isChecking) {
            System.out.println("RealTimeSyntaxChecker: Already checking, skipping");
            return; // Avoid concurrent checks
        }
        
        try {
            isChecking = true;
            System.out.println("RealTimeSyntaxChecker: Starting syntax check");
            
            // Run syntax check in UI thread for marker operations
            Display.getDefault().asyncExec(() -> {
                try {
                    doSyntaxCheck();
                } finally {
                    isChecking = false;
                    System.out.println("RealTimeSyntaxChecker: Syntax check completed");
                }
            });
            
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Error in syntax check: " + e.getMessage());
            e.printStackTrace();
            isChecking = false;
        }
    }
    
    private void doSyntaxCheck() {
        try {
            System.out.println("RealTimeSyntaxChecker: Starting doSyntaxCheck()");
            
            // Clear existing syntax error markers
            clearSyntaxMarkers();
            
            // Get current document content
            IDocument document = editor.getPublicSourceViewer().getDocument();
            String content = document.get();
            
            System.out.println("RealTimeSyntaxChecker: Document content length: " + content.length());
            System.out.println("RealTimeSyntaxChecker: First 100 chars: " + (content.length() > 100 ? content.substring(0, 100) : content));
            
            // For testing: add a syntax error to see if we can detect it
            String testContent = content + "\n\n// TEST SYNTAX ERROR\ninvalid syntax here @@@@";
            
            // Save content to temporary file for syntax checking
            File tempFile = createTempFileWithContent(testContent);
            System.out.println("RealTimeSyntaxChecker: Created temp file with test syntax error: " + tempFile.getAbsolutePath());
            
            try {
                // Perform syntax-only compilation
                checkSyntaxOnly(tempFile);
                
            } finally {
                // Clean up temporary file
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                    System.out.println("RealTimeSyntaxChecker: Cleaned up temp file");
                }
            }
            
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Syntax check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void clearSyntaxMarkers() {
        try {
            // Remove only syntax error markers (keep other markers intact)
            IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
            for (IMarker marker : markers) {
                String source = marker.getAttribute("syntaxChecker", null);
                if ("realTime".equals(source)) {
                    marker.delete();
                }
            }
        } catch (CoreException e) {
            System.err.println("Failed to clear syntax markers: " + e.getMessage());
        }
    }
    
    private File createTempFileWithContent(String content) throws IOException {
        File tempFile = File.createTempFile("rebeca_syntax_", ".rebeca");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }
    
    private void checkSyntaxOnly(File tempRebecaFile) {
        try {
            System.out.println("RealTimeSyntaxChecker: Starting checkSyntaxOnly()");
            
            // Check if components are initialized
            if (exceptionContainer == null) {
                System.err.println("RealTimeSyntaxChecker: exceptionContainer is null!");
                return;
            }
            if (modelCheckersFilesGenerator == null) {
                System.err.println("RealTimeSyntaxChecker: modelCheckersFilesGenerator is null!");
                return;
            }
            
            // Clear exception container
            exceptionContainer.clear();
            System.out.println("RealTimeSyntaxChecker: Cleared exception container");
            
            // Get compiler extensions (same as full compilation)
            Set<CompilerExtension> extensions = getCompilerExtensions();
            System.out.println("RealTimeSyntaxChecker: Got extensions: " + extensions.size());
            
            // Create minimal file generator properties for syntax checking only
            FileGeneratorProperties fileGeneratorProperties = createMinimalFileGeneratorProperties();
            System.out.println("RealTimeSyntaxChecker: Created file generator properties");
            
            // Perform syntax checking only (no file generation)
            // This is the key optimization - we only parse, don't generate C++ files
            // Create a temporary output directory that we'll ignore
            File tempOutputDir = File.createTempFile("rebeca_syntax_", "_tmp");
            tempOutputDir.delete();
            tempOutputDir.mkdirs();
            System.out.println("RealTimeSyntaxChecker: Created temp output dir: " + tempOutputDir.getAbsolutePath());
            
            try {
                System.out.println("RealTimeSyntaxChecker: Calling modelCheckersFilesGenerator.generateFiles()");
                System.out.println("RealTimeSyntaxChecker: - tempRebecaFile: " + tempRebecaFile.getAbsolutePath());
                System.out.println("RealTimeSyntaxChecker: - tempOutputDir: " + tempOutputDir.getAbsolutePath());
                System.out.println("RealTimeSyntaxChecker: - extensions: " + extensions);
                
                modelCheckersFilesGenerator.generateFiles(
                    tempRebecaFile,
                    null, // No property file for real-time checking
                    tempOutputDir, // Minimal temp directory
                    extensions,
                    fileGeneratorProperties
                );
                System.out.println("RealTimeSyntaxChecker: generateFiles() completed successfully");
            } finally {
                // Clean up temp directory
                if (tempOutputDir.exists()) {
                    deleteDirectory(tempOutputDir);
                    System.out.println("RealTimeSyntaxChecker: Cleaned up temp output dir");
                }
            }
            
            // Check for syntax errors and create markers
            System.out.println("RealTimeSyntaxChecker: Checking for exceptions. Empty? " + exceptionContainer.exceptionsIsEmpty());
            if (!exceptionContainer.exceptionsIsEmpty()) {
                System.out.println("RealTimeSyntaxChecker: Found exceptions, creating error markers");
                createSyntaxErrorMarkers();
            } else {
                System.out.println("RealTimeSyntaxChecker: No syntax errors found");
            }
            
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Syntax check compilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Set<CompilerExtension> getCompilerExtensions() {
        // Use the same extension retrieval logic as the main compiler
        try {
            return org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess
                .retrieveCompationExtension(file.getProject());
        } catch (Exception e) {
            // Fallback to minimal extensions
            Set<CompilerExtension> extensions = new java.util.HashSet<>();
            // Add core rebeca as default
            return extensions; // Will use default extensions from compiler
        }
    }
    
    private FileGeneratorProperties createMinimalFileGeneratorProperties() {
        // Create properties using the same logic as the main compiler but optimized for syntax checking
        try {
            FileGeneratorProperties fileGeneratorProperties = null;
            String languageType = CoreRebecaProjectPropertyPage.getProjectType(file.getProject());
            
            switch (languageType) {
            case "ProbabilisitcTimedRebeca":
            case "TimedRebeca":
                TimedRebecaFileGeneratorProperties timedProps = new TimedRebecaFileGeneratorProperties();
                if (TimedRebecaProjectPropertyPage.getProjectSemanticsModelIsTTS(file.getProject())) {
                    timedProps.setTTS(true);
                }
                fileGeneratorProperties = timedProps;
                break;
            default:
                fileGeneratorProperties = new FileGeneratorProperties();
            }
            
            // Set core version
            CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(file.getProject());
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
    
    private void createSyntaxErrorMarkers() {
        try {
            System.out.println("RealTimeSyntaxChecker: Creating syntax error markers");
            Set<Exception> exceptions = exceptionContainer.getExceptions().get(file.getRawLocation().toFile());
            System.out.println("RealTimeSyntaxChecker: Exceptions for file: " + (exceptions != null ? exceptions.size() : "null"));
            
            if (exceptions != null) {
                // Create marker for first error only (optimization)
                for (Exception exception : exceptions) {
                    System.out.println("RealTimeSyntaxChecker: Processing exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                    if (exception instanceof CodeCompilationException) {
                        CodeCompilationException cce = (CodeCompilationException) exception;
                        System.out.println("RealTimeSyntaxChecker: Creating marker for CodeCompilationException at line " + cce.getLine());
                        createSyntaxErrorMarker(cce);
                        break; // Stop at first error for performance
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Failed to create syntax error markers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createSyntaxErrorMarker(CodeCompilationException cce) {
        try {
            System.out.println("RealTimeSyntaxChecker: Creating IMarker.PROBLEM for: " + cce.getMessage());
            IMarker marker = file.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
            marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
            marker.setAttribute("syntaxChecker", "realTime"); // Tag for identification
            System.out.println("RealTimeSyntaxChecker: Successfully created error marker at line " + cce.getLine());
        } catch (CoreException e) {
            System.err.println("RealTimeSyntaxChecker: Failed to create syntax error marker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start real-time syntax checking for this document
     */
    public void startChecking(IDocument document) {
        System.out.println("RealTimeSyntaxChecker: Starting checking for document");
        document.addDocumentListener(this);
        System.out.println("RealTimeSyntaxChecker: Document listener added");
        
        // Perform initial syntax check
        System.out.println("RealTimeSyntaxChecker: Scheduling initial syntax check");
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
