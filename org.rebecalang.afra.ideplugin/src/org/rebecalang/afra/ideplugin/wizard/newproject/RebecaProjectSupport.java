package org.rebecalang.afra.ideplugin.wizard.newproject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.rebecalang.afra.ideplugin.nature.RebecaNature;
import org.rebecalang.afra.ideplugin.nature.SampleBuilder;
import org.rebecalang.afra.ideplugin.nature.TimedRebecaNature;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.afra.ideplugin.preference.TimedRebecaProjectPropertyPage;
import org.rebecalang.compiler.utils.CoreVersion;

public class RebecaProjectSupport {
	/**
	 * For this marvelous project we need to: - create the default Eclipse
	 * project - add the custom project nature - create the folder structure
	 *
	 * @param projectName
	 * @param location
	 * @param natureId
	 * @return
	 * @throws CoreException
	 */
	public static IProject createProject(String projectName, URI location, String type,
			CoreVersion version, boolean runInSafeMode, boolean exportStateSpace, boolean createSampleProjects) {
		Assert.isNotNull(projectName);
		Assert.isTrue(projectName.trim().length() > 0);

		IProject project = createBaseProject(projectName, location);

		try {
			CoreRebecaProjectPropertyPage.setProjectType(project, type);
			CoreRebecaProjectPropertyPage.setProjectLanguageVersion(project, version);
			CoreRebecaProjectPropertyPage.setProjectRunInSafeMode(project, runInSafeMode);
			CoreRebecaProjectPropertyPage.setProjectExportStateSpace(project, exportStateSpace);

			addNatures(project, type, version);
			
			createFiles(project, createSampleProjects, type);

		} catch (CoreException e) {
			e.printStackTrace();
			project = null;
		}
		return project;
	}
	
//	@SuppressWarnings("deprecation")
	private static void copyFile(IProject project, String fileName) {
//		StringWriter writer = new StringWriter();
		try {
//			IOUtils.copy(RebecaProjectSupport.class.getResource("/samples/" + fileName).openStream(), writer);
		    String fileContent = new String(
		    		RebecaProjectSupport.class.getResource("/samples/" + fileName).openStream().readAllBytes(), 
		    		StandardCharsets.UTF_8);

			IFile file = project.getFile("src/" + fileName);
			createFile(file, fileContent);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}					
	}

	private static void createFiles(IProject project, boolean createSampleProject, String type) throws CoreException {
		IFolder folder = project.getFolder("src");
		createFolder(folder);
		folder = project.getFolder("out");
		createFolder(folder);
		
		String projectName = project.getName();
		if (!createSampleProject) {
			IFile file = project.getFile("src/"+ projectName + ".property");
			createFile(file, "property { \n\n\n}");
			file = project.getFile("src/"+ projectName + ".rebeca");
			createFile(file);			
		} else {
			if (type.equals("CoreRebeca")) {
				copyFile(project, "DiningPhilosophers.rebeca");
				copyFile(project, "DiningPhilosophers.property");
				copyFile(project, "TrainController.rebeca");
				copyFile(project, "TrainController.property");
			} else if (type.equals("TimedRebeca")) {
				copyFile(project, "TinyOS-MACB.rebeca");
				copyFile(project, "TicketService.rebeca");
			}
		}

	}

	/**
	 * Just do the basics: create a basic project.
	 *
	 * @param location
	 * @param projectName
	 */
	private static IProject createBaseProject(String projectName, URI location) {
		// it is acceptable to use the ResourcesPlugin class
		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

		if (!newProject.exists()) {
			URI projectLocation = location;
			IProjectDescription desc = newProject.getWorkspace().newProjectDescription(newProject.getName());
			if (location != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
				projectLocation = null;
			}

			desc.setLocationURI(projectLocation);

			try {
				newProject.create(desc, null);
				if (!newProject.isOpen()) {
					newProject.open(null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return newProject;
	}

	private static void createFolder(IFolder folder) throws CoreException {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder) {
			createFolder((IFolder) parent);
		}
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
	}

	private static void createFile(IFile file) throws CoreException {
		createFile(file, "//Write your code here !");;
	}

	private static void createFile(IFile file, String data) throws CoreException {
		if (!file.exists()) {
			byte[] bytes = data.getBytes();
			InputStream source = new ByteArrayInputStream(bytes);
			file.create(source, IResource.NONE, null);
		}
	}

	private static void addNatures(IProject project, String type, CoreVersion version) throws CoreException {

		if (!project.hasNature(RebecaNature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();

			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(SampleBuilder.BUILDER_ID)) {
					return;
				}
			}
			ICommand[] newCommands = new ICommand[commands.length + 1];
			ICommand command = description.newCommand();
			command.setBuilderName(SampleBuilder.BUILDER_ID);
			newCommands[newCommands.length - 1] = command;
			description.setBuildSpec(newCommands);

			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = RebecaNature.NATURE_ID;

			if (type.equals("TimedRebeca") || type.equals("ProbabilisticTimedRebeca")) {
				prevNatures = description.getNatureIds();
				newNatures = new String[prevNatures.length + 1];
				System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
				newNatures[prevNatures.length] = TimedRebecaNature.NATURE_ID;
				TimedRebecaProjectPropertyPage.setProjectSemanticsModelIsTTS(project, true);
			}

			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}

}