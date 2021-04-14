package org.rebecalang.afra.ideplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.rebecalang.afra.ideplugin.handler.AbstractAnalysisHandler;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.rebecalang.afra.ideplugin"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
//		
//		 MultiStatus status = createMultiStatus(e.getLocalizedMessage(), e);
		// show error dialog
		plugin = this;

		ArrayList<String> testResult = checkGPlusPlus();
		String message = testResult.remove(0);
		if (message.equals("Ready")) {
			super.start(context);
		} else {
			if (testResult.isEmpty()) {
				testResult.add(message);
				message = "Wrong result in the execution of model checking file.";
			}
	        List<Status> childStatuses = new ArrayList<>();
	        for (String error : testResult) {
	            Status status = new Status(IStatus.ERROR,
	                    "org.rebecalang.afra.ideplugin", error);
	            childStatuses.add(status);
	        }

	        MultiStatus ms = new MultiStatus("org.rebecalang.afra.ideplugin",
	                IStatus.ERROR, childStatuses.toArray(new Status[] {}),
	                message, null);
			ErrorDialog.openError(Display.getDefault().getActiveShell(), 
					"Error", "Backend compilation does not work properly.", ms);

			super.stop(context);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative
	 * path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	private final static String sampleCode = 
			"#include <signal.h>\n" + 
			"#include <fstream>\n" + 
			"#include <iostream>\n" + 
			"#include <stdlib.h>\n" + 
			"#include <stdexcept>\n" + 
			"#include <thread> \n" + 
			"#include <chrono> \n" + 
			"using namespace std;\n" + 
			"void segmentationFaultHandler(int signum) {\n" + 
			"	exit(0);\n" + 
			"}\n" + 
			"void progressReport() {\n" + 
			"	cout << \"Ready\" << endl;\n" + 
			"}\n" + 
			"int main(int argc, char* argv[]) {\n" + 
			"	signal(SIGSEGV, segmentationFaultHandler);\n" +
			"	std::thread t1(progressReport);\n" + 
			"	t1.join();\n" + 
			"	return 0;\n" + 
			"}";

	private ArrayList<String> checkGPlusPlus() {
		ArrayList<String> result = new ArrayList<String>();
		try {
			File tempFile = File.createTempFile("AfraTestGPP", ".cpp");
			RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
			raf.writeBytes(sampleCode);
			raf.close();

			ArrayList<String> commandItems = new ArrayList<String>();
			commandItems.add("g++");
			commandItems.add(tempFile.getAbsolutePath());
			commandItems.add("-std=c++11");
			commandItems.add("-o");
			commandItems.add(tempFile.getParent() + "/execute");
			commandItems.add("-w");
			if (!AbstractAnalysisHandler.isWindows())
				commandItems.add("-pthread");
			String[] command = commandItems.toArray(new String[] {});

			Process p = Runtime.getRuntime().exec(command, null, tempFile.getParentFile());
			p.waitFor();
			result.addAll(readExecutionResultStream(p.getErrorStream()));
			result.addAll(readExecutionResultStream(p.getInputStream()));

			if (result.isEmpty()) {
				String executableFileName = tempFile.getParent();
				executableFileName = 
						AbstractAnalysisHandler.isWindows() ? 
								executableFileName + "\\execute.exe" : (executableFileName + "/execute");
				p = Runtime.getRuntime().exec(executableFileName, null, tempFile.getParentFile());
				p.waitFor();
				result.addAll(readExecutionResultStream(p.getErrorStream()));
				result.addAll(readExecutionResultStream(p.getInputStream()));
				if (result.size() != 1) {
					result.add(0, "The generated model checking file cannot be run correctly.");
				}
			} else {
				result.add(0, "Malfunctioning of g++ compiler. It is because of:\n"
						+ "  1) g++ bin foler is not in the path,\n"
						+ "  2) incomplete installation of g++,\n"
						+ "  3) installed g++ does not support c++11 threading.");
			}

		} catch (IOException e) {
			result.add("Error in access to files.");
			result.add(e.getMessage());
			for (StackTraceElement stackTrace : e.getStackTrace())
				result.add(stackTrace.toString());
			e.printStackTrace();
		} catch (InterruptedException e) {
			result.add("Error in the execution of processes.");
			result.add(e.getMessage());
			for (StackTraceElement stackTrace : e.getStackTrace())
				result.add(stackTrace.toString());
			e.printStackTrace();
		}
		return result;
	}

	private ArrayList<String> readExecutionResultStream(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		ArrayList<String> result = new ArrayList<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			result.add(line);
		}
		return result;
	}
}
