package org.rebecalang.afra.ideplugin.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.rebecalang.afra.ideplugin.preference.AbstractRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
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
		CompilationAndCodeGenerationProcess compilationAndCodeGenerationProcess = new 
				CompilationAndCodeGenerationProcess();
		try {
			CompilationStatus compilationStatus = compilationAndCodeGenerationProcess.compileAndGenerateCodes(shell, false);

			switch (compilationStatus) {
			case CANCELED:
				return;
			case SUCCESSFUL:
				break;
			case RESOURCE_DOES_NOE_EXIST:
				MessageDialog.openInformation(shell, "Compilation Error",
						"The corresponding Rebeca file does not exist.");
				return;
			case FAILED:
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.showView("org.eclipse.ui.views.ProblemView");
				return;
			}
		} catch (CoreException | InvocationTargetException | InterruptedException e) {
			MessageDialog.openError(shell, "Internal Error", e.getMessage());
			e.printStackTrace();
		}

		
		try {

			List<String> properiesNames = compilationAndCodeGenerationProcess.retrieveDefinedPropertyNames();

			PropertySelectionDialog dialog = new PropertySelectionDialog(shell, properiesNames);
			
			dialog.create();
			
			if (dialog.open() == TitleAreaDialog.OK) {
				
				String selectedPropertyName = dialog.getSelectedPropertyName();
				
				IProject project = CompilationAndCodeGenerationProcess.getProject();
				List<String> commandTerms = generateCommandTerms(
						project,
						compilationAndCodeGenerationProcess.getOutputFolder(), 
						selectedPropertyName
						);
				
				try {
					final Process process = Runtime.getRuntime().exec(commandTerms.toArray(new String[0]), 
							null,
							compilationAndCodeGenerationProcess.getOutputFolder());
					ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(shell) {
						@Override
						protected void cancelPressed() {
							super.cancelPressed();
							process.destroyForcibly();
						}
					};
					progressMonitorDialog.run(true, true,
							new ModelCheckingRunnableProgress(
									project,
									compilationAndCodeGenerationProcess.getOutputFolder(),
									process)
							);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException | InvocationTargetException | IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> generateCommandTerms(IProject project, File outputFolder, String selectedPropertyName) {
		List<String> commandTerms = new LinkedList<String>();
		
		commandTerms.add(outputFolder + File.separator + CompilationAndCodeGenerationProcess.getExecutableFileName());
		commandTerms.add("-o");
		commandTerms.add("output.xml");
		commandTerms.add("-g");
		commandTerms.add("progress");
		
		if(selectedPropertyName != null) {
			commandTerms.add("-p");
			commandTerms.add(selectedPropertyName);
		}
		
		if(AbstractRebecaProjectPropertyPage.getProjectType(project).equals("CoreRebeca")) {
			commandTerms.add("-d");
			commandTerms.add(String.valueOf(CoreRebecaProjectPropertyPage.getProjectMaxDepth(project)));
		}
		
		return commandTerms;
	}

	private void showResult(File outputFolder) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				AnalysisResultView view = (AnalysisResultView) ViewUtils.getViewPart(AnalysisResultView.class.getName());
				File modelCheckingResultFile = new File(outputFolder + File.separator +"output.xml");
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
								ceView.update(outputFolder + File.separator + "output.xml");
								
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
	
	private class ModelCheckingRunnableProgress implements IRunnableWithProgress {
		
		File outputFolder;
		
		Process process;

		private IProject project;
		
		public ModelCheckingRunnableProgress(IProject project, File outputFolder, Process process) {
			this.outputFolder = outputFolder;
			this.process = process;
			this.project = project;
		}

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
									new BufferedReader(new InputStreamReader(new FileInputStream(outputFolder + File.separator + "progress")));
							String line, backup = "";
							while ((line = reader.readLine())!= null) {
								backup = line;
							};
							monitor.subTask(backup + " are generated.");
							reader.close();
					  		schedule(2000);
						} catch (IOException e) {
							System.out.println("Progress file not found.");
						}
				  		return running ? org.eclipse.core.runtime.Status.OK_STATUS :
				  			org.eclipse.core.runtime.Status.CANCEL_STATUS;
					}
				};
				job.schedule();  
				
				process.waitFor();
				job.stop();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				if (reader.ready()) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				}
				
				if (CoreRebecaProjectPropertyPage.getProjectExportStateSpace(project)) {
					monitor.beginTask("Export State Space File", IProgressMonitor.UNKNOWN);
					File stateSpaceFile = new File(project.getRawLocation() + File.separator + "src" + File.separator
							+ outputFolder.getName() + ".statespace");
					IOUtils.copyLarge(new FileInputStream(outputFolder + File.separator + "statespace.xml"), 
							new FileOutputStream(stateSpaceFile));
					
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
				showResult(outputFolder);

			} catch (IOException | CoreException e1) {
				e1.printStackTrace();
			}
		}

	}
}
