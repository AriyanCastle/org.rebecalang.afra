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
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("Failed to initialize compiler components: " + e.getMessage());
        }
    }
    
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        // Cancel any pending syntax check
        cancelPendingCheck();
    }
    
    @Override
    public void documentChanged(DocumentEvent event) {
        // Schedule a new syntax check with debouncing
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
        
        debounceTimer = new Timer(true); // Daemon timer
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                performSyntaxCheck();
            }
        }, DEBOUNCE_DELAY_MS);
    }
    
    private void performSyntaxCheck() {
        if (isChecking) {
            return; // Avoid concurrent checks
        }
        
        try {
            isChecking = true;
            
            // Run syntax check in UI thread for marker operations
            Display.getDefault().asyncExec(() -> {
                try {
                    doSyntaxCheck();
                } finally {
                    isChecking = false;
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error in syntax check: " + e.getMessage());
            isChecking = false;
        }
    }
    
    private void doSyntaxCheck() {
        try {
            // Clear existing syntax error markers
            clearSyntaxMarkers();
            
            // Get current document content
            IDocument document = editor.getPublicSourceViewer().getDocument();
            String content = document.get();
            
            // Save content to temporary file for syntax checking
            File tempFile = createTempFileWithContent(content);
            
            try {
                // Perform syntax-only compilation
                checkSyntaxOnly(tempFile);
                
            } finally {
                // Clean up temporary file
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Syntax check failed: " + e.getMessage());
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
            // Clear exception container
            exceptionContainer.clear();
            
            // Get compiler extensions (same as full compilation)
            Set<CompilerExtension> extensions = getCompilerExtensions();
            
            // Create minimal file generator properties for syntax checking only
            FileGeneratorProperties fileGeneratorProperties = createMinimalFileGeneratorProperties();
            
            // Perform syntax checking only (no file generation)
            // This is the key optimization - we only parse, don't generate C++ files
            // Create a temporary output directory that we'll ignore
            File tempOutputDir = File.createTempFile("rebeca_syntax_", "_tmp");
            tempOutputDir.delete();
            tempOutputDir.mkdirs();
            
            try {
                modelCheckersFilesGenerator.generateFiles(
                    tempRebecaFile,
                    null, // No property file for real-time checking
                    tempOutputDir, // Minimal temp directory
                    extensions,
                    fileGeneratorProperties
                );
            } finally {
                // Clean up temp directory
                if (tempOutputDir.exists()) {
                    deleteDirectory(tempOutputDir);
                }
            }
            
            // Check for syntax errors and create markers
            if (!exceptionContainer.exceptionsIsEmpty()) {
                createSyntaxErrorMarkers();
            }
            
        } catch (Exception e) {
            System.err.println("Syntax check compilation failed: " + e.getMessage());
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
            Set<Exception> exceptions = exceptionContainer.getExceptions().get(file.getRawLocation().toFile());
            if (exceptions != null) {
                // Create marker for first error only (optimization)
                for (Exception exception : exceptions) {
                    if (exception instanceof CodeCompilationException) {
                        CodeCompilationException cce = (CodeCompilationException) exception;
                        createSyntaxErrorMarker(cce);
                        break; // Stop at first error for performance
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create syntax error markers: " + e.getMessage());
        }
    }
    
    private void createSyntaxErrorMarker(CodeCompilationException cce) {
        try {
            IMarker marker = file.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
            marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
            marker.setAttribute("syntaxChecker", "realTime"); // Tag for identification
        } catch (CoreException e) {
            System.err.println("Failed to create syntax error marker: " + e.getMessage());
        }
    }
    
    /**
     * Start real-time syntax checking for this document
     */
    public void startChecking(IDocument document) {
        document.addDocumentListener(this);
        
        // Perform initial syntax check
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
