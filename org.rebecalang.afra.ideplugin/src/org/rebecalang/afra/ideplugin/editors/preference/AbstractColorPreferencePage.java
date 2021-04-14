package org.rebecalang.afra.ideplugin.editors.preference;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaTextAttribute;
import org.rebecalang.afra.ideplugin.general.RebecaUIPlugin;

/**
 * This class is responsible for creating a preference page in Preferences
 * section of the applications for handling user's color preference in different
 * editors.
 * 
 * @author amshali
 * 
 */
public abstract class AbstractColorPreferencePage extends AbstractPreferencePage {
	protected TextAttribute[] attributes;

	protected List colorList;

	protected ColorSelector colorSelector;

	protected Button boldButton;

	protected Button italicButton;

	protected FieldListener fieldListener = new FieldListener();

	protected class TextAttribute {
		public String key;

		public String label;

		public TextAttribute(String key, String label) {
			this.key = key;
			this.label = label;
		}

		public void updateFields() {
			RGB color = RebecaUIPlugin.getPreferenceColor("Color." + key);
			colorSelector.setColorValue(color);

			boolean bold = RebecaUIPlugin.getPreferenceBoolean("Bold." + key);
			boldButton.setSelection(bold);

			boolean italic = RebecaUIPlugin.getPreferenceBoolean("Italic." + key);
			italicButton.setSelection(italic);
		}

		public void updatePreference() {
			RGB color = colorSelector.getColorValue();
			RebecaUIPlugin.setPreference("Color." + key, color);

			boolean bold = boldButton.getSelection();
			RebecaUIPlugin.setPreference("Bold." + key, bold);

			boolean italic = italicButton.getSelection();
			RebecaUIPlugin.setPreference("Italic." + key, italic);
		}

		public void loadDefault() {
			RebecaUIPlugin.setDefaultPreference("Color." + key);
			RebecaUIPlugin.setDefaultPreference("Bold." + key);
			RebecaUIPlugin.setDefaultPreference("Italic." + key);
		}
	}

	protected class FieldListener implements IPropertyChangeListener, SelectionListener {
		public void propertyChange(PropertyChangeEvent event) {
			updatePreference();
		}

		public void widgetSelected(SelectionEvent e) {
			updatePreference();
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			updatePreference();
		}

		private void updatePreference() {
			int idx = colorList.getSelectionIndex();
			if (idx >= 0)
				attributes[idx].updatePreference();
		}
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		createColorSection(composite);

		// initializeValues();
		return composite;
	}

	protected void createColorSection(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		colorList = new List(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		Composite attrField = new Composite(parent, SWT.NONE);
		attrField.setLayout(new GridLayout(2, false));

		new Label(attrField, SWT.NONE).setText("Color:");
		colorSelector = new ColorSelector(attrField);
		new Label(attrField, SWT.NONE).setText("Bold:");
		boldButton = new Button(attrField, SWT.CHECK);
		new Label(attrField, SWT.NONE).setText("Italic:");
		italicButton = new Button(attrField, SWT.CHECK);
		attributes[0].updateFields();

		String labels[] = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			labels[i] = attributes[i].label;
			// itemList.setItem(i, attributes[i].label);
		}
		colorList.setItems(labels);
		colorList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = colorList.getSelectionIndex();
				if (idx >= 0)
					attributes[idx].updateFields();
			}
		});
		colorList.select(0);

		colorSelector.addListener(fieldListener);
		boldButton.addSelectionListener(fieldListener);
		italicButton.addSelectionListener(fieldListener);
	}

	public boolean performOk() {
		super.performOk();
		RebecaTextAttribute.init();
		return true;
	}

	protected void performDefaults() {
		super.performDefaults();
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].loadDefault();
		}
		int idx = colorList.getSelectionIndex();
		if (idx >= 0)
			attributes[idx].updateFields();
	}

}
