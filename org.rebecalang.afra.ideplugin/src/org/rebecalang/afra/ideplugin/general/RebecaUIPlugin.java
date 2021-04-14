package org.rebecalang.afra.ideplugin.general;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.rebecalang.afra.ideplugin.Activator;

/**
 * The main plugin class to be used in the desktop.
 */
public class RebecaUIPlugin extends AbstractUIPlugin{

	public static final String PLUGIN_ID = "org.rebecalang.afra.plugin";

	public static final String CONSOLE_NAME = "org.rebecalang.afra.plugin.console";
	
	public static String Mode = "default";
	


	// The shared instance.
	private static RebecaUIPlugin plugin;

	/**
	 * The constructor.
	 */
	public RebecaUIPlugin() {
		System.out.println("RebecaUIPlugin");
	}
	/**
	 * Returns the shared instance.
	 */
	

	/**
	 * Returns the shared instance.
	 */
	public static RebecaUIPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Returns PreferenceStore
	 */
	public static IPreferenceStore getStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin preferences
	 */
	public static String getPreferenceString(String key) {
		return getStore().getString(key);
	}

	/**
	 * Returns the string from the plugin preferences
	 */
	public static boolean getPreferenceBoolean(String key) {
		return getStore().getBoolean(key);
	}

	/**
	 * Returns the RGB from the plugin preferences
	 */
	public static RGB getPreferenceColor(String key) {
		return PreferenceConverter.getColor(getStore(), key);
	}

	/**
	 * Returns the string list separated by "\n" from the plugin preferences
	 */
	public static List<String> getPreferenceStrings(String key) {
		String string = getPreferenceString(key);
		if (string == null)
			return null;

		int index = string.indexOf('\n');
		if (index >= 0) {
			// check version number
			if (!string.substring(0, index).equals("1"))
				return null;
		}

		List<String> list = new ArrayList<String>();
		int length = string.length();
		while (index >= 0 && index < length - 1) {
			int next = string.indexOf('\n', index + 1);
			if (next >= 0) {
				list.add(string.substring(index + 1, next));
			}
			index = next;
		}
		return list;
	}

	/**
	 * set the string to the plugin preferences
	 */
	public static void setPreference(String key, String value) {
		getStore().setValue(key, value);
	}

	/**
	 * set the string to the plugin preferences
	 */
	public static void setPreference(String key, boolean value) {
		getStore().setValue(key, value);
	}

	/**
	 * set the RGB to the plugin preferences
	 */
	public static void setPreference(String key, RGB rgb) {
		PreferenceConverter.setValue(getStore(), key, rgb);
	}

	/**
	 * set the string list separated by "\n"
	 */
	public static void setPreference(String key, List<Object> list) {
		StringBuffer value = new StringBuffer("1\n");
		for (Object i : list) {
			value.append(i.toString());
			value.append("\n");
		}
		setPreference(key, value.toString());
	}

	/**
	 * initialize default
	 */
	public static void setDefaultPreference(String key) {
		getStore().setToDefault(key);
	}

	public static void log(Throwable e) {
		if (e instanceof CoreException) {
			log(((CoreException) e).getStatus());
		}
		else {
			log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
		}
	}

	public static void log(IStatus status) {
		(plugin).getLog().log(status);
	}

	public static MessageConsole findConsole(String name) {
		IConsoleManager man = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = man.getConsoles();
		for (int i = 0; i < consoles.length; i++) {
			if (consoles[i].getName().equals(name))
				return (MessageConsole) consoles[i];
		}

		// if not exists, add new console
		MessageConsole newConsole = new MessageConsole(name, null);
		man.addConsoles(new IConsole[] { newConsole });
		return newConsole;
	}

	public void println(String msg) {
		MessageConsoleStream out = findConsole(CONSOLE_NAME).newMessageStream();
		out.println(msg);
	}

	public static PrintStream getConsolePrintStream() {
		return new PrintStream(findConsole(CONSOLE_NAME).newMessageStream());		
	}
	
	public static IOConsoleInputStream getConsoleInputStream() {
		return findConsole(CONSOLE_NAME).getInputStream();		
	}

	public static void clearConsole() {
		findConsole(CONSOLE_NAME).clearConsole();
	}

	public static void activateConsole() {
		findConsole(CONSOLE_NAME).activate();
	}

	public static void showProblemsView(IWorkbenchWindow window) {
		try {
			window.getActivePage().showView(IPageLayout.ID_PROBLEM_VIEW);
		}
		catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public static void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, null);
		throw new CoreException(status);
	}

}
