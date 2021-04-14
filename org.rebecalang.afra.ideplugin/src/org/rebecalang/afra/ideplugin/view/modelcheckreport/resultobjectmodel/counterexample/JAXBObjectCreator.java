package org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample;

public class JAXBObjectCreator {
	public static void main(String[] args) throws Throwable {
		//	        String[] arg = {
		//	        		"-p", "org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state", 
		//	        		"-d", "src/", 
		//	        		"src/org/rebecalang/afra/ideplugin/view/modelcheckreport/resultobjectmodel/counterexample/state.xsd"};

		//	        String[] arg = {
		//	        		"-p", "org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.transition", 
		//	        		"-d", "src/", 
		//	        		"src/org/rebecalang/afra/ideplugin/view/modelcheckreport/resultobjectmodel/counterexample/transition.xsd"};

		String[] arg = {
				"-p", "org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.transition", 
				"-d", "src/", 
		"src/org/rebecalang/afra/ideplugin/view/modelcheckreport/resultobjectmodel/counterexample/transition.xsd"};

		com.sun.tools.xjc.XJCFacade.main(arg);

	}
}
