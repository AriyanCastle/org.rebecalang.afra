package org.rebecalang.afra.ideplugin.preference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.rebecalang.compiler.utils.CompilerFeature;


public class AbstractRebecaProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private static final String HASHMAP_SIZE_TITLE = "Hash Map Size (2^x): ";

	protected static final int TEXT_FIELD_WIDTH = 20;

	private Button runInSafeMode;
	private Button exportStateSpace;
	private Text hashMapSizeText;
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	
	
	public static final String DEFAULT_HASHMAP_SIZE = "20";
	public static void setProjectHashtableSize(IProject project, String value) {
		setProjectAttribute(project, "hashMapSize", value);
	}
	public static String getProjectHashtableSize(IProject project) {
		return getProjectAttribute(project, "hashMapSize", DEFAULT_HASHMAP_SIZE);
	}
	
	public static void setProjectExportStateSpace(IProject project, boolean value) {
		setProjectAttribute(project, "exportStateSpace", Boolean.toString(value));
	}
	public static boolean getProjectExportStateSpace(IProject project) {
		return Boolean.parseBoolean(getProjectAttribute(project, "exportStateSpace", "false"));
	}
	
	public static void setProjectRunInSafeMode(IProject project, boolean value) {
		setProjectAttribute(project, "runInSafeMode", Boolean.toString(value));
	}
	public static boolean getProjectRunInSafeMode(IProject project) {
		return Boolean.parseBoolean(getProjectAttribute(project, "runInSafeMode", "true"));
	}

	protected static void setProjectAttribute(IProject project, String key, String value) {
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences projectPreferences = context.getNode("Rebeca");
		projectPreferences.put(key, value);
		try {
			projectPreferences.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
	protected static String getProjectAttribute(IProject project, String key, String defult) {
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences projectPreferences = context.getNode("Rebeca");
		return projectPreferences.get(key, defult);
	}

	public static void setProjectType(IProject project, String value) {
		setProjectAttribute(project, "projectType", value);
	}
	public static String getProjectType(IProject project) {
		return getProjectAttribute(project, "projectType", "CoreRebeca");
	}
	
	public static void setProjectLanguageVersion(IProject project, CompilerFeature value) {
		setProjectAttribute(project, "languageVersion", value.toString());
	}
	public static CompilerFeature getProjectLanguageVersion(IProject project) {
		return CompilerFeature.valueOf(getProjectAttribute(project, "languageVersion", CompilerFeature.CORE_2_1.toString()));
	}
	
	
	protected Control createContents(Composite parent) {
		
		// *********** General Section ***********//
		Composite container = new Composite(parent, SWT.NONE);

		
		Group runtimeconfig = new Group(container, SWT.NONE);
		runtimeconfig.setText("Runtime Configuration");
		runtimeconfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout clayout = new GridLayout();
		clayout.numColumns = 2;
		clayout.marginLeft = 10;
		runtimeconfig.setLayout(clayout);
		
		

		Label labelHashMapSize = new Label(runtimeconfig, SWT.NONE);
		labelHashMapSize.setText(HASHMAP_SIZE_TITLE);
		labelHashMapSize.pack();
		hashMapSizeText = new Text(runtimeconfig, SWT.BORDER | SWT.SINGLE);
		GridData gd2 = new GridData();
		gd2.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		hashMapSizeText.setLayoutData(gd2);
		hashMapSizeText.setText(getProjectHashtableSize(getProject()));
		hashMapSizeText.pack();
		
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		runInSafeMode = new Button(runtimeconfig, SWT.CHECK);
		runInSafeMode.setText("Run in safe mode");
		runInSafeMode.setSelection(getProjectRunInSafeMode(getProject()));
		new Label(runtimeconfig, SWT.NONE);
		
		exportStateSpace = new Button(runtimeconfig, SWT.CHECK);
		exportStateSpace.setText("Export State Space");
		exportStateSpace.setSelection(getProjectExportStateSpace(getProject()));

		return container;
	}


	protected void performDefaults() {
		runInSafeMode.setSelection(true);
		exportStateSpace.setSelection(false);
		hashMapSizeText.setText(DEFAULT_HASHMAP_SIZE);
	}

	protected IProject getProject() {
		return (IProject) getElement().getAdapter(IProject.class);
	}

	public boolean performOk() {
		setProjectExportStateSpace(getProject(), exportStateSpace.getSelection());
		setProjectRunInSafeMode(getProject(), runInSafeMode.getSelection());
		try {
			Integer.parseInt(hashMapSizeText.getText());
			setProjectHashtableSize(getProject(), hashMapSizeText.getText());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void performApply() {
		performOk();
		
	}
}
