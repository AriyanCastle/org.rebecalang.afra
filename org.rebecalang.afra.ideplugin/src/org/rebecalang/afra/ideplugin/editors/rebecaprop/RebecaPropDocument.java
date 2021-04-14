package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.rebecalang.afra.ideplugin.editors.GeneralDocument;
import org.rebecalang.afra.ideplugin.editors.GeneralRuleBasedPartitionScanner;

public class RebecaPropDocument extends GeneralDocument {

	public RebecaPropDocument(IProject project, IFile file) {
		super(project, file);
	}

	@Override
	public GeneralRuleBasedPartitionScanner createPartitionScanner() {
		return new RebecaPropPartitionScanner();
	}

}
