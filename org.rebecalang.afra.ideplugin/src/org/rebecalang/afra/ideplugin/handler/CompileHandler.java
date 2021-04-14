package org.rebecalang.afra.ideplugin.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.TimedRebecaProjectPropertyPage;
import org.rebecalang.compiler.propertycompiler.PropertyCodeCompilationException;
import org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.LTLDefinition;
import org.rebecalang.compiler.utils.CodeCompilationException;
import org.rebecalang.compiler.utils.CompilerFeature;
import org.rebecalang.rmc.AnalysisFeature;
import org.rebecalang.rmc.GenerateFiles;

public class CompileHandler extends AbstractAnalysisHandler {

	public final static String ID = CompileHandler.class.getName();

	private Object propertyModel;

	private boolean result;
	private boolean doesUserCancel;

	@CanExecute
	public boolean canExecute(EPartService partService) {
		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		return validateActiveFile(codeEditor);
	}

	@Execute
	public void execute(Shell shell) {

		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		codeEditor.getEditorSite().getWorkbenchWindow().getWorkbench().saveAllEditors(true);

		IFile activeFile = codeEditor.getEditorInput().getAdapter(IFile.class);
		if (activeFile.getFileExtension().equals("property")) {
			File rebecaFile = getRebecaFileFromPropertyFile(activeFile);
			activeFile = activeFile.getProject().getWorkspace().getRoot()
					.getFileForLocation(new Path(rebecaFile.getAbsolutePath()));
		}
		try {
			final IFile finalActiveFile = activeFile;
			CompilationStatus compilationStatus = PerformComilation(finalActiveFile, shell);
			switch (compilationStatus) {
			case CANCELED:
				return;
			case SUCCESSFUL:
				MessageDialog.openInformation(shell, "Compilation Report",
						activeFile.getName() + " is compiled successfully.");
				break;
			case FAILED:
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.showView("org.eclipse.ui.views.ProblemView");
				break;
			}
		} catch (InvocationTargetException | InterruptedException | CoreException e) {
			MessageDialog.openError(shell, "Internal Error", e.getMessage());
			e.printStackTrace();
		}
	}

	public CompilationStatus PerformComilation(final IFile activeFile, Shell shell)
			throws InvocationTargetException, InterruptedException {
		doesUserCancel = false;
		result = false;
		ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell) {
			@Override
			protected void cancelPressed() {
				super.cancelPressed();
				doesUserCancel = true;
			}
		};
		progressMonitorDialog.run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				IProject project = activeFile.getProject();
				boolean indeterminate = false;
				monitor.beginTask("Compiling the Rebeca model", indeterminate ? IProgressMonitor.UNKNOWN : 100);

				File propertyFile = getPropertyFileFromRebecaFile(activeFile);
				if (!propertyFile.exists())
					propertyFile = null;
				File outputFolder = new File(getOutputPath(activeFile));

				try {
					activeFile.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					if (propertyFile != null) {
						IFile propertyFileResource = project.getWorkspace().getRoot()
								.getFileForLocation(new Path(propertyFile.getAbsolutePath()));
						propertyFileResource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					}
					result = compileSec(activeFile, propertyFile, outputFolder);
					monitor.worked(10);
					if (!result) {
						monitor.done();
					} else {
						final File path = outputFolder;
						try {
							String[][] compilerCommand = generateCompilationCommands(path);
							int step = 80 / compilerCommand.length;
							monitor.subTask("Compiling auto generated C++ files");
							for (String[] command : compilerCommand) {
								if (doesUserCancel)
									return;
								Process p = Runtime.getRuntime().exec(command, null, path);
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
								monitor.worked(step);
							}

							monitor.subTask("Linking auto generated C++ files");
							String[] linkerCommand = generateLinkerCommands(path);
							Process p = Runtime.getRuntime().exec(linkerCommand, null, path);
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
							monitor.done();
						} catch (Exception e) {
							e.printStackTrace();
							result = false;
						}
					}
				} catch (IOException | CoreException e) {
					result = false;
					e.printStackTrace();
				}
			}
		});
		if (doesUserCancel)
			return CompilationStatus.CANCELED;
		if (!result)
			return CompilationStatus.FAILED;
		TextEditor codeEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IProject project = codeEditor.getEditorInput().getAdapter(IFile.class).getProject();
		storeDefinedPropertiesNames(project, activeFile.getName());
		return CompilationStatus.SUCCESSFUL;
	}

	private void storeDefinedPropertiesNames(IProject project, String fileName) {
		String definedProperties = "";
		if (propertyModel instanceof org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) {
			org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel model = (org.rebecalang.compiler.propertycompiler.corerebeca.objectmodel.PropertyModel) propertyModel;
			for (LTLDefinition definition : model.getLTLDefinitions())
				definedProperties += definition.getName() + ";";
		} else if (propertyModel instanceof org.rebecalang.compiler.propertycompiler.timedrebeca.objectmodel.PropertyModel) {
			// org.rebecalang.compiler.propertycompiler.timedrebeca.objectmodel.PropertyModel
			// model =
			// (org.rebecalang.compiler.propertycompiler.timedrebeca.objectmodel.PropertyModel)
			// propertyModel;
			// for (TCTLDefinition definition : model.getTCTLDefinitions())
			// definedProperties += definition.getName() + ";";
		}
		try {
			project.setPersistentProperty(new QualifiedName("definedProperties", fileName), definedProperties);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	// public static void showNoActiveRebecaFileErrorDialog(Shell shell) {
	// MessageDialog.openError(shell, "Error",
	// "No related Rebeca file is found.");
	// }

	private String[] generateLinkerCommands(File outputFolder) {
		String files[] = outputFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".o");
			}
		});
		
		String command[] = new String[files.length + (isWindows() ? 4 : 5)];

		command[0] = "g++";
		for (int i = 0; i < files.length; i++)
			command[i + 1] = files[i];
		command[files.length + 1] = "-w";
		command[files.length + 2] = "-o";
		command[files.length + 3] = "execute";
		if (!isWindows())
			command[files.length + 4] = "-pthread";
		return command;
	}

	private static String[][] generateCompilationCommands(File outputFolder) {
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

	public boolean compileSec(IFile rebecaFile, File propertyFile, File outputFolder)
			throws IOException, CoreException {
		boolean result = true;
		IProject project = rebecaFile.getProject();

		Set<CompilerFeature> compilerFeatures = new HashSet<CompilerFeature>();
		Set<AnalysisFeature> analysisFeatures = new HashSet<AnalysisFeature>();
		Properties properties = new Properties();

		CompilerFeature version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
		compilerFeatures.add(version);
		String type = CoreRebecaProjectPropertyPage.getProjectType(project);
		if (type.equals("TimedRebeca")) {
			compilerFeatures.add(CompilerFeature.TIMED_REBECA);
			if (TimedRebecaProjectPropertyPage.getProjectSemanticsModelIsTTS(project)) {
				analysisFeatures.add(AnalysisFeature.TTS);
			}
		} else if (type.equals("ProbabilisitcTimedRebeca")) {
			compilerFeatures.add(CompilerFeature.TIMED_REBECA);
			compilerFeatures.add(CompilerFeature.PROBABILISTIC_REBECA);
			analysisFeatures.add(AnalysisFeature.TTS);
		}
		if (CoreRebecaProjectPropertyPage.getProjectRunInSafeMode(project))
			analysisFeatures.add(AnalysisFeature.SAFE_MODE);
		if (CoreRebecaProjectPropertyPage.getProjectExportStateSpace(project)) {
			analysisFeatures.add(AnalysisFeature.EXPORT_STATE_SPACE);
			properties.setProperty("statespace",
					outputFolder.getAbsolutePath() + File.separatorChar + rebecaFile.getName() + ".statespace");
		}
		analysisFeatures.add(AnalysisFeature.PROGRESS_REPORT);

		clearFolder(outputFolder);
		GenerateFiles.getInstance().generateFiles(rebecaFile.getRawLocation().toFile(), propertyFile, outputFolder,
				compilerFeatures, analysisFeatures, properties);
		propertyModel = GenerateFiles.getInstance().getPropertyModel();
		IFile propertyFileResource = null;
		if (propertyFile != null)
			propertyFileResource = project.getWorkspace().getRoot()
					.getFileForLocation(new Path(propertyFile.getAbsolutePath()));
		
		for (Exception e : GenerateFiles.getInstance().getExceptionContainer().getWarnings()) {
			if (e instanceof CodeCompilationException) {
				CodeCompilationException cce = (CodeCompilationException) e;
				IMarker marker;
				if (cce instanceof PropertyCodeCompilationException)
					marker = createMarker(propertyFileResource, cce, true);
				else
					marker = createMarker(rebecaFile, cce, true);
				HashMap<String, Object> markers = new HashMap<String, Object>();
//				markers.put(IMarker.SEVERITY, marker.setAttribute(IMarker.SEVERITY, isWarning ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR);
				MarkerUtilities.setMessage(markers, cce.getMessage());
				MarkerUtilities.setLineNumber(markers, cce.getLine());

				MarkerUtilities.createMarker(rebecaFile, markers, "org.eclipse.ui.views.ProblemView");
			} else {
				e.printStackTrace();
			}
			
		}
		
		List<Exception> exceptions = new ArrayList<Exception>();
		exceptions.addAll(GenerateFiles.getInstance().getExceptionContainer().getExceptions());
		Collections.sort(exceptions, new Comparator<Exception>() {
			public int compare(Exception o1, Exception o2) {
				if (!(o1 instanceof CodeCompilationException))
					return 1;
				if (!(o2 instanceof CodeCompilationException))
					return -1;
				CodeCompilationException cce1 = (CodeCompilationException) o1, cce2 = (CodeCompilationException) o2;
				return cce1.getLine() < cce2.getLine() ? -1
						: cce1.getLine() > cce2.getLine() ? 1
								: cce1.getColumn() < cce2.getColumn() ? -1
										: cce1.getColumn() > cce2.getColumn() ? 1 : 0;
			}
		});
		for (Exception e : exceptions) {
			result = false;
			if (e instanceof CodeCompilationException) {
				CodeCompilationException cce = (CodeCompilationException) e;
				if (cce instanceof PropertyCodeCompilationException)
					createMarker(propertyFileResource, cce, false);
				else
					createMarker(rebecaFile, cce, false);
			} else {
				e.printStackTrace();
			}
		}
		return result;
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
				File delFile = new File(outputFolder.getAbsolutePath() + File.separatorChar + fileName);
				delFile.delete();
			}
		}
	}
}
