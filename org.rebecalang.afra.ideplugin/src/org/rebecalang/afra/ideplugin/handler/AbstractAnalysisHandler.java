package org.rebecalang.afra.ideplugin.handler;

import java.io.File;

//import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.editors.text.TextEditor;

public class AbstractAnalysisHandler {




	public boolean validateActiveFile(TextEditor codeEditor) {
		if (codeEditor == null)
			return false;
		IFile activeFile = codeEditor.getEditorInput().getAdapter(IFile.class);
		if (activeFile.getFileExtension().equals("rebeca"))
			return true;

		if(activeFile.getFileExtension().equals("property")) {
			File rebecaFile = CompilationAndCodeGenerationProcess.getRebecaFileFromPropertyFile(activeFile);
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
		CANCELED, SUCCESSFUL, FAILED, RESOURCE_DOES_NOE_EXIST 
	}
}