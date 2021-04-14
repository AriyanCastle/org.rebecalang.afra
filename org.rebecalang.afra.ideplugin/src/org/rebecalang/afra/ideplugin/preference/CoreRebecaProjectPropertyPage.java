package org.rebecalang.afra.ideplugin.preference;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


public class CoreRebecaProjectPropertyPage extends AbstractRebecaProjectPropertyPage {

	private static final String MAX_DEPTH_TITLE = "Max Depth: ";

	public static final String DEFAULT_MAX_DEPTH = "100000";


	public String CompileAlgorithm = "DFS";

	private Text maxDepthText;

	private Button dfs;
	private Button bfs;

	
	public static void setProjectMaxDepth(IProject project, String value) {
		setProjectAttribute(project, "dfs-maxdepth", value);
	}
	public static int getProjectMaxDepth(IProject project) {
		return Integer.parseInt(getProjectAttribute(project, "dfs-maxdepth", DEFAULT_MAX_DEPTH));
	}
	
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		Composite container = (Composite) super.createContents(parent);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;


		// *********** Compile Algorithm Section ***********//
		Group compileAlgorithm = new Group(container, SWT.SHADOW_IN);
		compileAlgorithm.setText("State Space Exploration Algorithm");
		compileAlgorithm.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout clayout = new GridLayout();
		compileAlgorithm.setLayout(clayout);
		clayout.numColumns = 3;
		clayout.marginLeft = 10;

		dfs = new Button(compileAlgorithm, SWT.RADIO);
		dfs.setText("DFS");
		dfs.setSelection(true);
		dfs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				compileAlgorithmChanged();
			}
		});
		dfs.pack();

		Label label = new Label(compileAlgorithm, SWT.NONE);
		label.setText(MAX_DEPTH_TITLE);
		label.pack();
		maxDepthText = new Text(compileAlgorithm, SWT.BORDER | SWT.SINGLE);
		GridData gd1 = new GridData();
		gd1.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		maxDepthText.setLayoutData(gd1);
		maxDepthText.setText(Integer.toString(getProjectMaxDepth(getProject())));
		maxDepthText.pack();

		bfs = new Button(compileAlgorithm, SWT.RADIO);
		bfs.setText("BFS");
		bfs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				compileAlgorithmChanged();
			}
		});
		bfs.pack();
		bfs.setEnabled(false);

		return container;
	}

	public boolean performOk() {
		if (!super.performOk())
			return false;
		try {
			Integer.parseInt(maxDepthText.getText());
			setProjectMaxDepth(getProject(), maxDepthText.getText());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	

	protected void compileAlgorithmChanged() {
		CompileAlgorithm = "";
		if (dfs.getSelection()) {
			CompileAlgorithm = "DFS";
			maxDepthText.setEnabled(true);
		} else {
			CompileAlgorithm = "BFS";
			maxDepthText.setEnabled(false);
		}
	}

	protected void performDefaults() {
		super.performDefaults();
		maxDepthText.setText(DEFAULT_MAX_DEPTH);
		maxDepthText.setEnabled(true);
		dfs.setSelection(true);
		bfs.setSelection(false);

		bfs.setEnabled(false);
	}

}
