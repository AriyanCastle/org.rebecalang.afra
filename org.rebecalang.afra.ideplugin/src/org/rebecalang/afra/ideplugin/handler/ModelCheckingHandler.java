package org.rebecalang.afra.ideplugin.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.AbstractRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.propertypages.PropertySelectionDialog;
import org.rebecalang.afra.ideplugin.view.AnalysisResultView;
import org.rebecalang.afra.ideplugin.view.CounterExampleGraphView;
import org.rebecalang.afra.ideplugin.view.ViewUtils;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.analysisresult.ModelCheckingReport;

public class ModelCheckingHandler extends AbstractAnalysisHandler {
	
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

		String outputFolder;
		IFile activeFile = (IFile)codeEditor.getEditorInput().getAdapter(IFile.class);
		
		if (activeFile.getFileExtension().equals("property")) {
			File rebecaFile = getRebecaFileFromPropertyFile(activeFile);
			activeFile = activeFile.getProject().getWorkspace().getRoot().getFileForLocation(new org.eclipse.core.runtime.Path(rebecaFile.getAbsolutePath()));
		}
		outputFolder = getOutputPath(activeFile);
		
		CompileHandler compileHandler = new CompileHandler();
		try {

			String executableFileName = "execute" + ((System.getProperty("os.name").contains("Windows")) ? ".exe" : "");

			Path execFile = Paths.get(outputFolder + executableFileName);
			Path rebecaFile = Paths.get(activeFile.getRawLocation().toString());
			Path propertyFile = Paths.get(getPropertyFileFromRebecaFile(activeFile).getAbsolutePath());
			try {
				BasicFileAttributes execAttr = Files.readAttributes(execFile, BasicFileAttributes.class);
				BasicFileAttributes rebecaAttr = Files.readAttributes(rebecaFile, BasicFileAttributes.class);
				BasicFileAttributes propAttr = propertyFile.toFile().exists() ? 
						Files.readAttributes(propertyFile, BasicFileAttributes.class) :
						rebecaAttr;
				if (rebecaAttr.lastModifiedTime().toMillis() > execAttr.lastModifiedTime().toMillis() || 
						propAttr.lastModifiedTime().toMillis() > execAttr.lastModifiedTime().toMillis() ) {

					CompilationStatus comilationResult = compileHandler.PerformComilation(activeFile, shell);
					if (comilationResult == CompilationStatus.FAILED)
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView("org.eclipse.ui.views.ProblemView");
					if (comilationResult != CompilationStatus.SUCCESSFUL)
						return;
				}
			} catch (IOException e) {
				CompilationStatus comilationResult = compileHandler.PerformComilation(activeFile, shell);
				if (comilationResult == CompilationStatus.FAILED)
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.showView("org.eclipse.ui.views.ProblemView");
				if (comilationResult != CompilationStatus.SUCCESSFUL)
					return;
			}
			String definedProperties = activeFile.getProject().getPersistentProperty(new QualifiedName("definedProperties", activeFile.getName()));
			String projectType = AbstractRebecaProjectPropertyPage.getProjectType(activeFile.getProject());
			String[] properiesNameList = definedProperties.isEmpty() ? new String[0] : definedProperties.split(";");

			PropertySelectionDialog dialog = new PropertySelectionDialog(shell, properiesNameList);
			dialog.create();
			if (dialog.open() == TitleAreaDialog.OK) {
				String selectedPropertyName = dialog.getSelectedPropertyName();
				String[] params;
				int paramSize = 5;
				if (selectedPropertyName != null)
					paramSize += 2;
				if (projectType.equals("CoreRebeca"))
					paramSize += 2;
				params = new String[paramSize];
				if (projectType.equals("CoreRebeca")) {
					params[--paramSize] = "" + CoreRebecaProjectPropertyPage.getProjectMaxDepth(activeFile.getProject());
					params[--paramSize] = "-d";
				}
				if (selectedPropertyName != null) {
					params[--paramSize] = selectedPropertyName;
					params[--paramSize] = "-p";
				}
				params[0] = outputFolder + executableFileName;
				params[1] = "-o";
				params[2] = "output.xml";
				params[3] = "-g";
				params[4] = "progress";
				for(String part : params)
					System.out.println(part);
				final String finalOutputFolder = outputFolder;
				final IFile finalActiveFile = activeFile;
				try {
					final Process p = Runtime.getRuntime().exec(params, null,
							new File(finalOutputFolder));
					ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell) {

						@Override
						protected void cancelPressed() {
							super.cancelPressed();
							p.destroyForcibly();
						}
						
					};
					progressMonitorDialog.run(true, true,
							new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException, InterruptedException {
									monitor.beginTask("Performing Model Checking",
											IProgressMonitor.UNKNOWN);
									try {
										RepeatingJob job = new RepeatingJob() {			
											protected IStatus run(IProgressMonitor localMonitor){ 
												try {
													BufferedReader reader = 
															new BufferedReader(new InputStreamReader(new FileInputStream(finalOutputFolder + "progress")));
													String line, backup = "";
													while ((line = reader.readLine())!= null) {
														backup = line;
													};
													monitor.subTask(backup + " are generated.");
													reader.close();
											  		schedule(2000);
												} catch (IOException e) {
													e.printStackTrace();
												}
										  		return running ? org.eclipse.core.runtime.Status.OK_STATUS :
										  			org.eclipse.core.runtime.Status.CANCEL_STATUS;
											}
										};
										job.schedule();  
										
										p.waitFor();
										job.stop();
										BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
										if (reader.ready()) {
											String line;
											while ((line = reader.readLine()) != null) {
												System.out.println(line);
											}
										}
										if (CoreRebecaProjectPropertyPage.getProjectExportStateSpace(finalActiveFile.getProject())) {
											monitor.beginTask("Export State Space File",
													IProgressMonitor.UNKNOWN);
											File stateSpaceFile = new File(finalActiveFile.getProject().getLocation().toOSString() + 
													File.separatorChar + "src" + File.separatorChar + 
													extractFileName(finalActiveFile) + ".statespace");
											IOUtils.copyLarge(new FileInputStream(finalOutputFolder + "statespace.xml"), 
													new FileOutputStream(stateSpaceFile));
											finalActiveFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
										}
										showResult(finalOutputFolder);

									} catch (IOException | CoreException e1) {
										e1.printStackTrace();
									}
								}
							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException | CoreException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void showResult(final String finalOutputFolder) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				AnalysisResultView view = (AnalysisResultView) ViewUtils.getViewPart(AnalysisResultView.class.getName());
				File modelCheckingResultFile = new File(finalOutputFolder + "output.xml");
				if (modelCheckingResultFile.exists() && modelCheckingResultFile.length() > 0) {
					try {
						JAXBContext jaxbContext;
						jaxbContext = JAXBContext.newInstance(ModelCheckingReport.class.getPackage().getName());
						Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
						ModelCheckingReport modelCheckingReport = (ModelCheckingReport) unmarshaller.unmarshal(modelCheckingResultFile);
						view.setReport(modelCheckingReport);
						if (modelCheckingReport != null)
							if (!modelCheckingReport.getCheckedProperty().getResult().equals("satisfied") &&
									!modelCheckingReport.getCheckedProperty().getResult().equals("search stack overflow") &&
									!modelCheckingReport.getCheckedProperty().getResult().endsWith("(heap overflow)") &&
									modelCheckingReport.getSystemInfo().getReachedStates().intValue() != 0) {
								ViewUtils.counterExampleVisible(true);
								CounterExampleGraphView ceView = 
										(CounterExampleGraphView) ViewUtils.getViewPart(CounterExampleGraphView.class.getName());
								ceView.update(finalOutputFolder + "output.xml");
								
							}
							else {
								ViewUtils.counterExampleVisible(false);
							}
					} catch (JAXBException | IOException e) {
						e.printStackTrace();
					}
				}
				view.update();
			}
		});
	}
	public class RepeatingJob extends Job {
		protected boolean running = true;
		public RepeatingJob() {
			super("Repeating Job");
		}
		protected IStatus run(IProgressMonitor monitor) {
			schedule(60000);
			return Status.OK_STATUS;
		}
		public boolean shouldSchedule() {
			return running;
		}
		public void stop() {
			running = false;
		}
	}
}
