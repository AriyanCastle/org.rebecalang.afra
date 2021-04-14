package org.rebecalang.afra.ideplugin.editors;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public abstract class GeneralFileDocumentProvider extends FileDocumentProvider {
	
	protected abstract GeneralDocument getDocument(IProject project, IFile file);
	
	protected GeneralDocument createDocument(Object element) throws CoreException
	{
		GeneralDocument document = null;

		if (element instanceof IFileEditorInput)
		{
			// find project
			IFileEditorInput input = (IFileEditorInput)element;
			IFile file = input.getFile();

			IContainer parent = file.getParent();
			while (parent instanceof IFolder)
			{
				parent = parent.getParent();
			}
			if (parent instanceof IProject)
			{
				document = getDocument((IProject)parent, file);
				if (!setDocumentContent(document, input, getEncoding(element)))
					document = null;
			}
		}
		else if (element instanceof IPathEditorInput)
		{
			IPathEditorInput input = (IPathEditorInput) element;
			document = getDocument(null, null);
			FileInputStream contentStream = null;
			try
			{
				contentStream = new FileInputStream(input.getPath().toFile());
				setDocumentContent(document, contentStream, getEncoding(element));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				document = null;
			}
		}
		if (document != null)
		{
			GeneralRuleBasedPartitionScanner scanner = document.createPartitionScanner();
			IDocumentPartitioner partitioner = new FastPartitioner(scanner,
					scanner.getContentTypes());
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
		return document;
	}
}
