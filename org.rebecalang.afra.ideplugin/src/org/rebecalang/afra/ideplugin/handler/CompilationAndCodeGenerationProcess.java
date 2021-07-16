package org.rebecalang.afra.ideplugin.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.rebecalang.afra.ideplugin.handler.AbstractAnalysisHandler.CompilationStatus;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.TimedRebecaProjectPropertyPage;
import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.LTLDefinition;
import org.rebecalang.compiler.propertycompiler.generalrebeca.objectmodel.PropertyModel;
import org.rebecalang.compiler.utils.CodeCompilationException;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.ExceptionContainer;
import org.rebecalang.rmc.FileGeneratorProperties;
import org.rebecalang.rmc.ModelCheckersFilesGenerator;
import org.rebecalang.rmc.RMCConfig;
import org.rebecalang.rmc.timedrebeca.TimedRebecaFileGeneratorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CompilationAndCodeGenerationProcess {
	
	
	@Autowired
	private ExceptionContainer exceptionContainer;

	@Autowired
	private ModelCheckersFilesGenerator modelCheckersFilesGenerator;
	
	public final static String PROPERTY_NAMES_FILE_NAME = "properties.txt";
	
	private boolean codeGenerationCanceledByUser;
	
	private boolean errorInFiles;
	
	private File outputFolder;
	
	CompilationAndCodeGenerationProcess() {
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

	public CompilationStatus compileAndGenerateCodes(Shell shell, boolean force) throws InvocationTargetException, InterruptedException {
		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		codeEditor.getEditorSite().getWorkbenchWindow().getWorkbench().saveAllEditors(true);

		IFile rebecaFile = codeEditor.getEditorInput().getAdapter(IFile.class);
		IFile propertyFile = rebecaFile;
		if (rebecaFile.getFileExtension().equals("property")) {
			rebecaFile = propertyFile.getProject().getWorkspace().getRoot()
					.getFileForLocation(new org.eclipse.core.runtime.Path(getRebecaFileFromPropertyFile(propertyFile).getAbsolutePath()));
		} else {
			propertyFile = rebecaFile.getProject().getWorkspace().getRoot()
					.getFileForLocation(new org.eclipse.core.runtime.Path(getPropertyFileFromRebecaFile(rebecaFile).getAbsolutePath()));
		}
		if(!rebecaFile.exists()) {
			MessageDialog.openError(shell, "Rebeca file with name ", rebecaFile.getName() + " does not exits.");
			return CompilationStatus.RESOURCE_DOES_NOE_EXIST;
		}

		outputFolder = new File(getOutputPath(rebecaFile));
		
		try {
			if(!force) {
				if (!filesAreUpdated(rebecaFile, propertyFile))
					return CompilationStatus.SUCCESSFUL;
			}
			
			CompilationProgressMonitor compilationProgressMonitor = new CompilationProgressMonitor(rebecaFile, propertyFile, shell);
			compilationProgressMonitor.run();
			if (codeGenerationCanceledByUser)
				return CompilationStatus.CANCELED;
			if (errorInFiles)
				return CompilationStatus.FAILED;

			storeDefinedPropertyNames(modelCheckersFilesGenerator.getPropertyModel());
		} catch (IOException e) {
			e.printStackTrace();
			return CompilationStatus.FAILED;
		}
		return CompilationStatus.SUCCESSFUL;
	}

	public File getOutputFolder() {
		return outputFolder;
	}
	
	private void storeDefinedPropertyNames(PropertyModel propertyModel) throws IOException {
		RandomAccessFile propertyNames = new RandomAccessFile(outputFolder + File.separator +
				PROPERTY_NAMES_FILE_NAME, "rw");
		propertyNames.setLength(0);
		if (propertyModel instanceof org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) {
			org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel model = 
					(org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) propertyModel;
			for (LTLDefinition definition : model.getLTLDefinitions())
				propertyNames.writeBytes(definition.getName() + "\n");
		}
		propertyNames.close();
	}

	public List<String> retrieveDefinedPropertyNames() throws IOException {
		List<String> retValue = new ArrayList<String>();
		RandomAccessFile propertyNamesFile = new RandomAccessFile(outputFolder + File.separator + PROPERTY_NAMES_FILE_NAME, "r");
		String line;
		while((line = propertyNamesFile.readLine()) != null) {
			retValue.add(line.trim());
		}
		propertyNamesFile.close();
		return retValue;
	}

	protected boolean filesAreUpdated(IFile rebecaFile, IFile propertyFile) throws IOException {

		Path execFilePath = Paths.get(outputFolder + File.separator + getExecutableFileName());
		Path propertyNamesFilePath = Paths.get(outputFolder + File.separator + PROPERTY_NAMES_FILE_NAME);
		Path rebecaFilePath = Paths.get(rebecaFile.getRawLocation().toString());
		Path propertyFilePath = Paths.get(propertyFile.getRawLocation().toString());

		if(!execFilePath.toFile().exists())
			return true;
		if(!propertyNamesFilePath.toFile().exists())
			return true;
		BasicFileAttributes execAttr = Files.readAttributes(execFilePath, BasicFileAttributes.class);
		BasicFileAttributes rebecaAttr = Files.readAttributes(rebecaFilePath, BasicFileAttributes.class);
		BasicFileAttributes propAttr = propertyFile.exists() ? 
				Files.readAttributes(propertyFilePath, BasicFileAttributes.class) :
				rebecaAttr;
		if (rebecaAttr.lastModifiedTime().toMillis() > execAttr.lastModifiedTime().toMillis() || 
				propAttr.lastModifiedTime().toMillis() > execAttr.lastModifiedTime().toMillis() ) {
			return true;
		}
		return false;
	}

	public static String getExecutableFileName() {
		return "execute" + ((System.getProperty("os.name").contains("Windows")) ? ".exe" : "");
	}
	
	public static File getRebecaFileFromPropertyFile(IFile propertyFile) {
		return getFileFromByReplacingExtension(propertyFile, "rebeca");
	}
	public static File getPropertyFileFromRebecaFile(IFile rebecaFile) {
		return getFileFromByReplacingExtension(rebecaFile, "property");
	}
	public static String extractFileName(IFile finalActiveFile) {
		return finalActiveFile.getName().substring(0, finalActiveFile.getName().lastIndexOf("."));
	}
	public static String getOutputPath(final IFile file) {
		return file.getProject().getLocation().toOSString() + File.separatorChar + "out" + File.separatorChar 
				+ extractFileName(file) + File.separatorChar;
	}
	public static File getFileFromByReplacingExtension(IFile file, String newExtension) {
		return new File(file.getRawLocation().toString().substring(0,
				file.getRawLocation().toString().lastIndexOf(file.getFileExtension())) + newExtension);
	}
	public static IProject getProject() {
		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IFile file = codeEditor.getEditorInput().getAdapter(IFile.class);
		
		return file.getProject();
	}
	
	public static Set<CompilerExtension> retrieveCompationExtension (IProject project) {
		Set<CompilerExtension> returnValue = new HashSet<CompilerExtension>();
		String languageType = CoreRebecaProjectPropertyPage.getProjectType(project);
		switch (languageType) {
		case "ProbabilisitcTimedRebeca":
			returnValue.add(CompilerExtension.PROBABILISTIC_REBECA);
		case "TimedRebeca":
			returnValue.add(CompilerExtension.TIMED_REBECA);
		}
		return returnValue;
	}

	private class CompilationProgressMonitor extends ProgressMonitorDialog {
		
		IFile rebecaFile, propertyFile;
		
		public CompilationProgressMonitor(IFile rebecaFile, IFile propertyFile, Shell shell) {
			super(shell);
			this.rebecaFile = rebecaFile;
			this.propertyFile = propertyFile;
		}

		public void run() throws InvocationTargetException, InterruptedException {
			this.run(true, true, new CompilationRunner());
		}

		@Override
		protected void cancelPressed() {
			super.cancelPressed();
			codeGenerationCanceledByUser = true;
		}

		private IMarker createErrorMarker(IResource file, CodeCompilationException cce) {
			return createMarker(file, cce, IMarker.SEVERITY_ERROR);
		}
		
		private IMarker createWarningMarker(IResource file, CodeCompilationException cce) {
			return createMarker(file, cce, IMarker.SEVERITY_WARNING);
		}
		
		private IMarker createMarker(IResource file, CodeCompilationException cce, int severity) {
			try {
				IMarker marker = file.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, severity);
				marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
				marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());
				return marker;
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return null;
		}

		private class CompilationRunner implements IRunnableWithProgress {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				
				boolean indeterminate = false;
				monitor.beginTask("Compiling the Rebeca model", indeterminate ? IProgressMonitor.UNKNOWN : 100);

				try {
					
					deleteMarkersFromFiles();
					
					clearFolder(outputFolder);
					
					errorInFiles = !generateModelCheckingFiles(outputFolder);
					
					monitor.worked(10);
					if (errorInFiles) {
						monitor.done();
					} else {
						try {
							String[][] compilerCommand = generateCompilationCommands(outputFolder);
							int step = 80 / compilerCommand.length;
							monitor.subTask("Compiling auto generated C++ files");
							for (String[] command : compilerCommand) {
								if (codeGenerationCanceledByUser)
									return;
								runACommand(command, outputFolder);
								monitor.worked(step);
							}

							monitor.subTask("Linking auto generated C++ files");
							String[] linkerCommand = generateLinkerCommands(outputFolder);
							runACommand(linkerCommand, outputFolder);
							monitor.done();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (IOException | CoreException e) {
					e.printStackTrace();
				}
			}
			
			private void runACommand(String[] command, File outputFolder) throws IOException, InterruptedException {
				Process p = Runtime.getRuntime().exec(command, null, outputFolder);
				p.waitFor();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}				
			}

			private FileGeneratorProperties retreiveFileGenerationProperties() {
				IProject project = rebecaFile.getProject();
				FileGeneratorProperties fileGeneratorProperties = null;
				String languageType = CoreRebecaProjectPropertyPage.getProjectType(project);
				switch (languageType) {
				case "ProbabilisitcTimedRebeca":
				case "TimedRebeca":
					TimedRebecaFileGeneratorProperties timedRebecaFileGeneratorProperties
						= new TimedRebecaFileGeneratorProperties();
					if (TimedRebecaProjectPropertyPage.getProjectSemanticsModelIsTTS(project)) {
						timedRebecaFileGeneratorProperties.setTTS(true);
					}
					
					fileGeneratorProperties = timedRebecaFileGeneratorProperties;
					break;
				default:
					fileGeneratorProperties =  new FileGeneratorProperties();
				}
				
				CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
				fileGeneratorProperties.setCoreVersion(version);
				
				if (CoreRebecaProjectPropertyPage.getProjectRunInSafeMode(project))
					fileGeneratorProperties.setSafeMode(true);
				if (CoreRebecaProjectPropertyPage.getProjectExportStateSpace(project)) {
					String stateSpaceFile = rebecaFile.getName();
					stateSpaceFile = stateSpaceFile.substring(0, stateSpaceFile.lastIndexOf('.')) + ".statespace";
					fileGeneratorProperties.setExportStateSpaceTargetFile(
							rebecaFile.getParent().getFullPath() + stateSpaceFile);
				}

				fileGeneratorProperties.setProgressReport(true);
				return fileGeneratorProperties;
			}

			private void deleteMarkersFromFiles() throws CoreException {
				rebecaFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				rebecaFile.createMarker(IMarker.BOOKMARK); //To enforce persisting the effect of deleting markers
				if (propertyFile.exists()) {
					propertyFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					propertyFile.createMarker(IMarker.BOOKMARK); //To enforce persisting the effect of deleting markers
				}
			}

			public boolean generateModelCheckingFiles(File outputFolder)
					throws IOException, CoreException {
				boolean result = true;

				FileGeneratorProperties fileGeneratorProperties = 
						retreiveFileGenerationProperties();
				
				Set<CompilerExtension> extensions = 
						CompilationAndCodeGenerationProcess.retrieveCompationExtension(rebecaFile.getProject());
	
				modelCheckersFilesGenerator.generateFiles(
						rebecaFile.getRawLocation().toFile(), 
						propertyFile.exists() ? propertyFile.getRawLocation().toFile(): null, 
						outputFolder,
						extensions, 
						fileGeneratorProperties);
				
				if(!exceptionContainer.exceptionsIsEmpty()) {
					result = false;
					associateMarkersWithFile(rebecaFile, 
							exceptionContainer.getExceptions().get(rebecaFile.getRawLocation().toFile()),
							exceptionContainer.getWarnings().get(rebecaFile.getRawLocation().toFile()));
					if(propertyFile.exists())
						if (exceptionContainer.getExceptions().get(propertyFile.getRawLocation().toFile()) != null)
							associateMarkersWithFile(propertyFile, 
									exceptionContainer.getExceptions().get(propertyFile.getRawLocation().toFile()),
									exceptionContainer.getWarnings().get(propertyFile.getRawLocation().toFile()));
				}
				
				return result;
			}

			private void associateMarkersWithFile(IFile file, Set<Exception> exceptions, Set<Exception> warnings) {

				if(exceptions != null)
					for(Exception exception : exceptions) {
						if (exception instanceof CodeCompilationException) {
							CodeCompilationException cce = (CodeCompilationException) exception;
							createErrorMarker(file, cce);
						} else {
							exception.printStackTrace();
						}					
					}
				if(warnings != null)
					for(Exception warning : warnings) {
						if (warning instanceof CodeCompilationException) {
							CodeCompilationException cce = (CodeCompilationException) warning;
							createWarningMarker(file, cce);
						} else {
							warning.printStackTrace();
						}					
					}
			}

			private String[] generateLinkerCommands(File outputFolder) {
				String files[] = outputFolder.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".o");
					}
				});
				
				String command[] = new String[files.length + (AbstractAnalysisHandler.isWindows() ? 4 : 5)];

				command[0] = "g++";
				for (int i = 0; i < files.length; i++)
					command[i + 1] = files[i];
				command[files.length + 1] = "-w";
				command[files.length + 2] = "-o";
				command[files.length + 3] = "execute";
				if (!AbstractAnalysisHandler.isWindows())
					command[files.length + 4] = "-pthread";
				return command;
			}

			private String[][] generateCompilationCommands(File outputFolder) {
				String files[] = outputFolder.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".cpp");
					}
				});
				String command[][] = new String[files.length][5];
				for (int cnt = 0; cnt < files.length; cnt++) {
					command[cnt][0] = "g++";
					command[cnt][1] = "-std=c++11";
					command[cnt][2] = "-c";
					command[cnt][3] = files[cnt];
					command[cnt][4] = "-w";
				}
				return command;
			}

			private void clearFolder(File outputFolder) {
				if (outputFolder.exists()) {
					String files[] = outputFolder.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".o") || name.toLowerCase().endsWith(".h")
									|| name.toLowerCase().endsWith(".cpp");
						}
					});

					for (String fileName : files) {
						File delFile = new File(outputFolder + File.separator + fileName);
						delFile.delete();
					}
				}
			}
}
	}

}
