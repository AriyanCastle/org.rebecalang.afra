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

public class RealTimeSyntaxChecker implements IDocumentListener {
    
    private static final int DEBOUNCE_DELAY_MS = 600;
    
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
        System.out.println("RealTimeSyntaxChecker: Document about to be changed");
        cancelPendingCheck();
    }
    
    @Override
    public void documentChanged(DocumentEvent event) {
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
        debounceTimer = new Timer(true);
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
            return;
        }
        
        try {
            isChecking = true;
            System.out.println("RealTimeSyntaxChecker: Starting syntax check");
            
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
        
            clearSyntaxMarkers();
            IDocument document = editor.getPublicSourceViewer().getDocument();
            String content = document.get();
            
            File tempFile = createTempFileWithContent(content);
            
            try {
                checkSyntaxOnly(tempFile);
            } finally {
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
            if (exceptionContainer == null) {
                System.err.println("RealTimeSyntaxChecker: exceptionContainer is null!");
                return;
            }
            if (modelCheckersFilesGenerator == null) {
                System.err.println("RealTimeSyntaxChecker: modelCheckersFilesGenerator is null!");
                return;
            }
            
            exceptionContainer.clear();
            
            Set<CompilerExtension> extensions = getCompilerExtensions();
            
            FileGeneratorProperties fileGeneratorProperties = createMinimalFileGeneratorProperties();
    
            File tempOutputDir = File.createTempFile("rebeca_syntax_", "_tmp");
            tempOutputDir.delete();
            tempOutputDir.mkdirs();
            
            try {
                modelCheckersFilesGenerator.generateFiles(
                    tempRebecaFile,
                    null,
                    tempOutputDir,
                    extensions,
                    fileGeneratorProperties
                );
            } finally {
                if (tempOutputDir.exists()) {
                    deleteDirectory(tempOutputDir);
                }
            }
            
            if (!exceptionContainer.exceptionsIsEmpty()) {
                createSyntaxErrorMarkers();
            }
            
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Syntax check compilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Set<CompilerExtension> getCompilerExtensions() {
        try {
            Set<CompilerExtension> extensions = org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess
                .retrieveCompationExtension(file.getProject());
            
            if (extensions.isEmpty()) {
                System.out.println("RealTimeSyntaxChecker: Extensions empty, using core Rebeca (no extensions needed)");
            }
            
            return extensions;
        } catch (Exception e) {
            System.err.println("RealTimeSyntaxChecker: Failed to get extensions: " + e.getMessage());
            Set<CompilerExtension> extensions = new java.util.HashSet<>();
            System.out.println("RealTimeSyntaxChecker: Using fallback empty extensions (core Rebeca)");
            return extensions;
        }
    }
    
    private FileGeneratorProperties createMinimalFileGeneratorProperties() {
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
            
            CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(file.getProject());
            fileGeneratorProperties.setCoreVersion(version);
            
            fileGeneratorProperties.setSafeMode(false);
            fileGeneratorProperties.setProgressReport(false);
            
            return fileGeneratorProperties;
            
        } catch (Exception e) {
            return new FileGeneratorProperties();
        }
    }
    
    private void createSyntaxErrorMarkers() {
        try {
            
            Set<Exception> exceptions = exceptionContainer.getExceptions().get(file.getRawLocation().toFile());
                    
            if (exceptions == null && !exceptionContainer.getExceptions().isEmpty()) {
                for (Set<Exception> tempExceptions : exceptionContainer.getExceptions().values()) {
                    if (tempExceptions != null && !tempExceptions.isEmpty()) {
                        exceptions = tempExceptions;
                        break;
                    }
                }
            }
            
            if (exceptions != null) {
                for (Exception exception : exceptions) {
                    System.out.println("RealTimeSyntaxChecker: Processing exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                    if (exception instanceof CodeCompilationException) {
                        CodeCompilationException cce = (CodeCompilationException) exception;
                        createSyntaxErrorMarker(cce);
                        break;
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
            IMarker marker = file.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
            marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
            marker.setAttribute("syntaxChecker", "realTime");
        } catch (CoreException e) {
            System.err.println("RealTimeSyntaxChecker: Failed to create syntax error marker: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void startChecking(IDocument document) {
        document.addDocumentListener(this);
        scheduleDelayedSyntaxCheck();
    }

    public void stopChecking(IDocument document) {
        document.removeDocumentListener(this);
        cancelPendingCheck();
        clearSyntaxMarkers();
    }
    
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
        }
    }
    
    public void dispose() {
        cancelPendingCheck();
        isChecking = false;
    }
}
