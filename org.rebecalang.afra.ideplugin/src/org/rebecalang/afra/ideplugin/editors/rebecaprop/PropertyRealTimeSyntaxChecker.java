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
        initializeCompilerComponents();
    }
    
    private void initializeCompilerComponents() {
        try {
            @SuppressWarnings("resource")
            ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
            AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to initialize compiler components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        System.out.println("PropertyRealTimeSyntaxChecker: Document about to be changed");
        cancelPendingCheck();
    }
    
    @Override
    public void documentChanged(DocumentEvent event) {
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
        
        debounceTimer = new Timer(true);
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                performSyntaxCheck();
            }
        }, DEBOUNCE_DELAY_MS);
    }
    
    private void performSyntaxCheck() {
        if (isChecking) {
            return;
        }
        
        try {
            isChecking = true;

            Display.getDefault().asyncExec(() -> {
                try {
                    doSyntaxCheck();
                } finally {
                    isChecking = false;
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
            clearSyntaxMarkers();
            
            File rebecaFile = getCorrespondingRebecaFile();
            if (rebecaFile == null || !rebecaFile.exists()) {
                createMissingRebecaFileMarker();
                return;
            }
            
            IDocument document = editor.getPublicSourceViewer().getDocument();
            String content = document.get();
            
            File tempPropertyFile = createTempFileWithContent(content, ".property");
            System.out.println("PropertyRealTimeSyntaxChecker: Created temp property file: " + tempPropertyFile.getAbsolutePath());
            
            try {
                checkSyntaxOnly(rebecaFile, tempPropertyFile);
            } finally {
                if (tempPropertyFile != null && tempPropertyFile.exists()) {
                    tempPropertyFile.delete();
                }
            }
            
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Syntax check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private File getCorrespondingRebecaFile() {
        try {
            return CompilationAndCodeGenerationProcess.getRebecaFileFromPropertyFile(propertyFile);
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Error getting corresponding rebeca file: " + e.getMessage());
            return null;
        }
    }
    
    private void clearSyntaxMarkers() {
        try {
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
        return tempFile;
    }
    
    private void checkSyntaxOnly(File rebecaFile, File tempPropertyFile) {
        try {
            System.out.println("PropertyRealTimeSyntaxChecker: Starting checkSyntaxOnly()");
            
            if (exceptionContainer == null) {
                return;
            }
            if (modelCheckersFilesGenerator == null) {
                return;
            }
            
            exceptionContainer.clear();
            
            Set<CompilerExtension> extensions = getCompilerExtensions();
            System.out.println("PropertyRealTimeSyntaxChecker: Got extensions: " + extensions.size());
            
            FileGeneratorProperties fileGeneratorProperties = createMinimalFileGeneratorProperties();
            System.out.println("PropertyRealTimeSyntaxChecker: Created file generator properties");
            
            File tempOutputDir = File.createTempFile("property_syntax_", "_tmp");
            tempOutputDir.delete();
            tempOutputDir.mkdirs();
            
            try {
                modelCheckersFilesGenerator.generateFiles(
                    rebecaFile,                
                    tempPropertyFile,      
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
                createSyntaxErrorMarkers(tempPropertyFile);
            }
            
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Syntax check compilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Set<CompilerExtension> getCompilerExtensions() {
        try {
            Set<CompilerExtension> extensions = CompilationAndCodeGenerationProcess
                .retrieveCompationExtension(propertyFile.getProject());
            
            if (extensions.isEmpty()) {
                System.out.println("PropertyRealTimeSyntaxChecker: Extensions empty, using core Rebeca (no extensions needed)");
            }
            
            return extensions;
        } catch (Exception e) {
            Set<CompilerExtension> extensions = new java.util.HashSet<>();
            System.out.println("PropertyRealTimeSyntaxChecker: Using fallback empty extensions (core Rebeca)");
            return extensions;
        }
    }
    
    private FileGeneratorProperties createMinimalFileGeneratorProperties() {
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
            
            CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(propertyFile.getProject());
            fileGeneratorProperties.setCoreVersion(version);
            
            fileGeneratorProperties.setSafeMode(false); 
            fileGeneratorProperties.setProgressReport(false); 
            
            return fileGeneratorProperties;
            
        } catch (Exception e) {
            return new FileGeneratorProperties();
        }
    }
    
    private void createSyntaxErrorMarkers(File tempPropertyFile) {
        try {
            Set<Exception> exceptions = exceptionContainer.getExceptions().get(tempPropertyFile);
            
            if (exceptions != null) {
                for (Exception exception : exceptions) {
                    if (exception instanceof CodeCompilationException) {
                        CodeCompilationException cce = (CodeCompilationException) exception;
                        createSyntaxErrorMarker(cce);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to create syntax error markers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createSyntaxErrorMarker(CodeCompilationException cce) {
        try {
            IMarker marker = propertyFile.createMarker(IMarker.PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
            marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
            marker.setAttribute("syntaxChecker", "realTimeProperty");
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
        } catch (CoreException e) {
            System.err.println("PropertyRealTimeSyntaxChecker: Failed to create missing rebeca file marker: " + e.getMessage());
        }
    }
    

    public void startChecking(IDocument document) {
        System.out.println("PropertyRealTimeSyntaxChecker: Starting checking for document");
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
