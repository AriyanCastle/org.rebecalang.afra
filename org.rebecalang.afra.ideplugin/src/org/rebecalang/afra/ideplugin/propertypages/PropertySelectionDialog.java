package org.rebecalang.afra.ideplugin.propertypages;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class PropertySelectionDialog extends TitleAreaDialog {

	String[] propertiesNames;
	private int selectedIndex = 0;
	
	public PropertySelectionDialog(Shell parentShell, String[] propertiesNames) {
		super(parentShell);
		this.propertiesNames = propertiesNames;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Property Selection");
		setMessage("Which property do you want to check?", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		createFirstName(container);

		return area;
	}

	private void createFirstName(Composite container) {
		GridData dataFirstName = new GridData();
		dataFirstName.grabExcessHorizontalSpace = true;
		dataFirstName.horizontalAlignment = GridData.FILL;
		dataFirstName.heightHint = 70;

		List properties = new List(container, SWT.BORDER | SWT.SINGLE);
		properties.add("Default[Assertions]");
		for(String properyName : propertiesNames)
			properties.add(properyName);
		properties.setLayoutData(dataFirstName);
		properties.setSelection(0);
		properties.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				selectedIndex = properties.getSelectionIndex();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	public String getSelectedPropertyName() {
		if (selectedIndex == 0)
			return null;
		return propertiesNames[selectedIndex - 1];
	}
}