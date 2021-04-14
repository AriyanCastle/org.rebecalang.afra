package org.rebecalang.afra.ideplugin.view;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
//import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.state.State;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.transition.Messageserver;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.transition.Time;
import org.rebecalang.afra.ideplugin.view.modelcheckreport.resultobjectmodel.counterexample.transition.Transition;

public class CounterExampleGraphView extends ViewPart {

	public final static String COMPOSIT_ID = "org.rebecalang.afra.ideplugin.compositepart.counterexample";
	public static final String COUNTER_EXAMPLE_START_TAG = "<counter-example-trace>";
	public static final String COUNTER_EXAMPLE_END_TAG = "</counter-example-trace>";
	public static final String STATE_END_TAG = "</state>";
	
	private final static int NODE_START_X = 100;
	private final static int NODE_START_Y = 20;
	private final static int STEP_Y = 60;

	public class CounterExampleStateSpecification {
		private long fileDescriptor;
		private int lineNumber;
		private String fileName;
		private String id;

		public long getFileDescriptor() {
			return fileDescriptor;
		}

		public void setFileDescriptor(long fileDescriptor) {
			this.fileDescriptor = fileDescriptor;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String toString() {
			return id;
		}
	}

	private CounterExampleStateSpecification extractOneStateInfo(RandomAccessFile analysisResultFile, String fileName)
			throws IOException {
		CounterExampleStateSpecification cess = new CounterExampleStateSpecification();
		cess.setFileDescriptor(analysisResultFile.getFilePointer());
		cess.setFileName(fileName);

		String line;
		line = analysisResultFile.readLine().trim();
		int startIndex = line.indexOf("id") + 4;
		int endIndex = line.indexOf('\"', startIndex);
		cess.setId(line.substring(startIndex, endIndex));

		int lineCounter = 2;
		while (!(line = analysisResultFile.readLine()).trim().equals(STATE_END_TAG)) {
			lineCounter++;
		}
		cess.setLineNumber(lineCounter);
		return cess;
	}

	public void update(String analysisResultFileName) throws IOException, JAXBException {
		for (Control control : parent.getChildren()) {
			control.dispose();
		}

		Graph graph = new Graph(parent, SWT.NONE);
		graph.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Graph graph = (Graph) e.getSource();
				if (graph.getSelection().isEmpty())
					return;
				Object object = graph.getSelection().get(0);
				if (!(object instanceof GraphNode))
					return;
				GraphNode node = (GraphNode) object;
				CounterExampleStateSpecification cess = (CounterExampleStateSpecification) node.getData();
				try {
					RandomAccessFile file = new RandomAccessFile(cess.getFileName(), "r");
					file.seek(cess.getFileDescriptor());
					StringBuffer stateContent = new StringBuffer();
					for (int cnt = 0; cnt < cess.getLineNumber(); cnt++) {
						stateContent.append(file.readLine());
					}
					file.close();

					JAXBContext jaxbContext;
					jaxbContext = JAXBContext.newInstance(State.class.getPackage().getName());
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
					State state = (State) 
							unmarshaller.unmarshal(new StringReader(stateContent.toString()));

					StateInCounterExampleView view = 
							(StateInCounterExampleView) ViewUtils.getViewPart(StateInCounterExampleView.class.getName());
					view.update(state);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (JAXBException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		Hashtable<String, GraphNode> states = new Hashtable<>();
		RandomAccessFile analysisResultFile = new RandomAccessFile(analysisResultFileName, "r");
		while(!analysisResultFile.readLine().trim().equals(COUNTER_EXAMPLE_START_TAG));
		
		String line;
		
		int nodeY = NODE_START_Y;

		CounterExampleStateSpecification cess = extractOneStateInfo(analysisResultFile, analysisResultFileName);
		GraphNode newNode = new GraphNode(graph, SWT.NONE, cess);
		newNode.setLocation(NODE_START_X, nodeY);
		newNode.setText(cess.getId());
		states.put(cess.getId(), newNode);		
		nodeY += STEP_Y;
		while(true) {
			line = analysisResultFile.readLine();
			if (line == null)
				break;
			JAXBContext jaxbContext;
			jaxbContext = JAXBContext.newInstance(Transition.class.getPackage().getName());
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Transition transition = (Transition) 
					unmarshaller.unmarshal(new StringReader(line));
			String source = transition.getSource();
			String destination = transition.getDestination();

			boolean repeatedState = states.containsKey(destination);
			if (!repeatedState) {
				if(analysisResultFile.getFilePointer() != analysisResultFile.length()) {
					cess = extractOneStateInfo(analysisResultFile, analysisResultFileName);
					newNode = new GraphNode(graph, SWT.NONE, cess);
					newNode.setLocation(NODE_START_X, nodeY);
					newNode.setText(cess.getId());
					states.put(cess.getId(), newNode);			
					nodeY += STEP_Y;
				}
			}
			
			GraphConnection gc = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, 
					states.get(source), states.get(destination));
			if (source.equals(destination))
				gc.setCurveDepth(50);
			String messageText = "";
			if (transition.getMessageserver() != null) {
				Messageserver messageserver = transition.getMessageserver();
				messageText = messageserver.getOwner() + "." + messageserver.getTitle() + " from " +
						messageserver.getSender();				
			} else if(transition.getTime() != null) {
				Time time = transition.getTime();
				messageText = "Time progress by " + time.getProgressOfTime().toString() + " units";
			}
			if (transition.getExecutionTime() != null)
				messageText += " @(" + transition.getExecutionTime() + ")";
			gc.setText(messageText);
			gc.setLineColor(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			
			if (repeatedState) {
				Point location = states.get(source).getLocation();
				states.get(source).setLocation(location.x() + 100, location.y());
				location = states.get(destination).getLocation();
				states.get(destination).setLocation(location.x() + 100, location.y());
				break;
			} else {
				long backup = analysisResultFile.getFilePointer();
				if(!(line = analysisResultFile.readLine()).trim().equals(COUNTER_EXAMPLE_END_TAG))
					analysisResultFile.seek(backup);
				else
					break;
			}
		}
		analysisResultFile.close();
		parent.layout(true);
	}

	public void update() {
		for (Control control : parent.getChildren()) {
			control.dispose();
		}
	}

	private Composite parent;

	@PostConstruct
	public void createPartControl(Composite parent) {
		this.parent = parent;
		update();
		// Graph will hold all other objects
	}


	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}
}