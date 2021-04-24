package org.rebecalang.afra.ideplugin.handler;

import java.io.File;

//import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.editors.text.TextEditor;
import org.rebecalang.compiler.utils.CodeCompilationException;

public class AbstractAnalysisHandler {

	protected static IMarker createMarker(IResource file, CodeCompilationException cce, boolean isWarning) {
		try {
			IMarker marker = file.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, isWarning ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, cce.getMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, cce.getLine());

			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static File getFileFromByReplacingExtension(IFile file, String newExtension) {
		return new File(file.getRawLocation().toString().substring(0,
				file.getRawLocation().toString().lastIndexOf(file.getFileExtension())) + newExtension);
	}
	
	public File getRebecaFileFromPropertyFile(IFile propertyFile) {
		return getFileFromByReplacingExtension(propertyFile, "rebeca");
	}
	public File getPropertyFileFromRebecaFile(IFile rebecaFile) {
		return getFileFromByReplacingExtension(rebecaFile, "property");
	}
	public String extractFileName(IFile finalActiveFile) {
		return finalActiveFile.getName().substring(0, finalActiveFile.getName().lastIndexOf("."));
	}
	public String getOutputPath(final IFile file) {
		return file.getProject().getLocation().toOSString() + File.separatorChar + "out" + File.separatorChar 
				+ extractFileName(file) + File.separatorChar;
	}

	public boolean validateActiveFile(TextEditor codeEditor) {
		if (codeEditor == null)
			return false;
		IFile activeFile = codeEditor.getEditorInput().getAdapter(IFile.class);
		if (activeFile.getFileExtension().equals("rebeca"))
			return true;

		if(activeFile.getFileExtension().equals("property")) {
			File rebecaFile = getRebecaFileFromPropertyFile(activeFile);
			return rebecaFile.exists();
		} 
		return false;
	}

//	public static boolean isUnix(String OS) {
//		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
//	}
	
	
    private static String OS = System.getProperty("os.name").toLowerCase();
    public static boolean IS_WINDOWS = (OS.indexOf("win") >= 0);
    public static boolean IS_MAC = (OS.indexOf("mac") >= 0);
    public static boolean IS_UNIX = (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    
	public static boolean isWindows() {
		return IS_WINDOWS;
	}
	
	public enum CompilationStatus {
		CANCELED, SUCCESSFUL, FAILED 
	}
}