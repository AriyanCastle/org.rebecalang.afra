package org.rebecalang.afra.ideplugin.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public abstract class GeneralDocument extends AbstractDocument {
	/**
	 * project which has this Rebeca source file
	 */
	public GeneralDocument(IProject project, IFile file)
	{
		super(project, file);
	}

	public abstract GeneralRuleBasedPartitionScanner createPartitionScanner();
}
