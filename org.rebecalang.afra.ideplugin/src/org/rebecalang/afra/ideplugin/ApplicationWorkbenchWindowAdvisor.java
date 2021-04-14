package org.rebecalang.afra.ideplugin;

import java.io.PrintStream;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.rebecalang.afra.ideplugin.view.ViewUtils;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setTitle("Rebeca IDE"); //$NON-NLS-1$
        getWindowConfigurer().getWindow().getShell().setMaximized(true);
    }
    
    @Override
    public void postWindowOpen()
    {
        getWindowConfigurer().getWindow().getShell().setMaximized(true);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().
					getActivePage().showView("org.eclipse.ui.views.ProblemView");
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().
					getActivePage().showView("org.eclipse.ui.navigator.ProjectExplorer");
			MessageConsole myConsole = new MessageConsole("StandardIOConsole", Activator
					.getImageDescriptor("icons/log.png"));
			ConsolePlugin.getDefault().getConsoleManager()
					.addConsoles(new IConsole[] { myConsole });
			final MessageConsoleStream stream = myConsole.newMessageStream();
			final PrintStream myS = new PrintStream(stream);
			System.setOut(myS);
			System.setErr(myS);

//			CounterExampleGraphView view = (CounterExampleGraphView) ViewUtils.getViewPart(CounterExampleGraphView.class.getName());
//			view.update();
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().
//			getActivePage().hideView(view);			
			
			ViewUtils.counterExampleVisible(false);
					
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}						
    } 
}
