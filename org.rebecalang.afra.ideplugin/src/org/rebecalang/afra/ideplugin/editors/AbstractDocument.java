package org.rebecalang.afra.ideplugin.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.Document;

public class AbstractDocument extends Document {
	/**
	 * project which has this Rebeca source file
	 */
	protected IProject project;
	protected IFile file;

	public AbstractDocument(IProject project, IFile file)
	{
		super();
		this.project = project;
		this.file = file;
	}

	public IProject getProject()
	{
		return project;
	}

	public IFile getFile()
	{
		return file;
	}
}
