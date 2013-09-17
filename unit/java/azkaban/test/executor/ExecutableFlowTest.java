package azkaban.test.executor;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutableFlowBase;
import azkaban.executor.ExecutableNode;
import azkaban.executor.ExecutionOptions;
import azkaban.executor.ExecutionOptions.FailureAction;
import azkaban.executor.Status;
import azkaban.flow.Flow;
import azkaban.project.Project;
import azkaban.utils.DirectoryFlowLoader;
import azkaban.utils.JSONUtils;

public class ExecutableFlowTest {
	private Project project;
	
    @Before
    public void setUp() throws Exception {
		Logger logger = Logger.getLogger(this.getClass());
    	DirectoryFlowLoader loader = new DirectoryFlowLoader(logger);
    	loader.loadProjectFlow(new File("unit/executions/embedded"));
    	Assert.assertEquals(0, loader.getErrors().size());
    	
    	project = new Project(11, "myTestProject");
    	project.setFlows(loader.getFlowMap());
    	project.setVersion(123);
    }
    
    @After
    public void tearDown() throws Exception {
    }
	
	@Test
	public void testExecutorFlowCreation() throws Exception {
		Flow flow = project.getFlow("jobe");
		Assert.assertNotNull(flow);
		
		ExecutableFlow exFlow = new ExecutableFlow(project, flow);
		Assert.assertNotNull(exFlow.getExecutableNode("joba"));
		Assert.assertNotNull(exFlow.getExecutableNode("jobb"));
		Assert.assertNotNull(exFlow.getExecutableNode("jobc"));
		Assert.assertNotNull(exFlow.getExecutableNode("jobd"));
		Assert.assertNotNull(exFlow.getExecutableNode("jobe"));
		
		Assert.assertFalse(exFlow.getExecutableNode("joba") instanceof ExecutableFlowBase);
		Assert.assertTrue(exFlow.getExecutableNode("jobb") instanceof ExecutableFlowBase);
		Assert.assertTrue(exFlow.getExecutableNode("jobc") instanceof ExecutableFlowBase);
		Assert.assertTrue(exFlow.getExecutableNode("jobd") instanceof ExecutableFlowBase);
		Assert.assertFalse(exFlow.getExecutableNode("jobe") instanceof ExecutableFlowBase);
		
		ExecutableFlowBase jobbFlow = (ExecutableFlowBase)exFlow.getExecutableNode("jobb");
		ExecutableFlowBase jobcFlow = (ExecutableFlowBase)exFlow.getExecutableNode("jobc");
		ExecutableFlowBase jobdFlow = (ExecutableFlowBase)exFlow.getExecutableNode("jobd");

		Assert.assertEquals("innerFlow", jobbFlow.getFlowId());
		Assert.assertEquals("jobb", jobbFlow.getId());
		Assert.assertEquals(4, jobbFlow.getExecutableNodes().size());
		
		Assert.assertEquals("innerFlow", jobcFlow.getFlowId());
		Assert.assertEquals("jobc", jobcFlow.getId());
		Assert.assertEquals(4, jobcFlow.getExecutableNodes().size());
		
		Assert.assertEquals("innerFlow", jobdFlow.getFlowId());
		Assert.assertEquals("jobd", jobdFlow.getId());
		Assert.assertEquals(4, jobdFlow.getExecutableNodes().size());
	}
	
	@Test
	public void testExecutorFlowJson() throws Exception {
		Flow flow = project.getFlow("jobe");
		Assert.assertNotNull(flow);
		
		ExecutableFlow exFlow = new ExecutableFlow(project, flow);
		
		Object obj = exFlow.toObject();
		String exFlowJSON = JSONUtils.toJSON(obj);
		@SuppressWarnings("unchecked")
		Map<String,Object> flowObjMap = (Map<String,Object>)JSONUtils.parseJSONFromString(exFlowJSON);
		
		ExecutableFlow parsedExFlow = ExecutableFlow.createExecutableFlowFromObject(flowObjMap);
		testEquals(exFlow, parsedExFlow);
	}
	
	@Test
	public void testExecutorFlowJson2() throws Exception {
		Flow flow = project.getFlow("jobe");
		Assert.assertNotNull(flow);
		
		ExecutableFlow exFlow = new ExecutableFlow(project, flow);
		exFlow.setExecutionId(101);
		exFlow.setAttempt(2);
		exFlow.setDelayedExecution(1000);
		
		ExecutionOptions options = new ExecutionOptions();
		options.setConcurrentOption("blah");
		options.setDisabledJobs(Arrays.asList(new String[] {"bee", null, "boo"}));
		options.setFailureAction(FailureAction.CANCEL_ALL);
		options.setFailureEmails(Arrays.asList(new String[] {"doo", null, "daa"}));
		options.setSuccessEmails(Arrays.asList(new String[] {"dee", null, "dae"}));
		options.setPipelineLevel(2);
		options.setPipelineExecutionId(3);
		options.setNotifyOnFirstFailure(true);
		options.setNotifyOnLastFailure(true);
		
		HashMap<String, String> flowProps = new HashMap<String,String>();
		flowProps.put("la", "fa");
		options.setFlowParameters(flowProps);
		exFlow.setExecutionOptions(options);
		
		Object obj = exFlow.toObject();
		String exFlowJSON = JSONUtils.toJSON(obj);
		@SuppressWarnings("unchecked")
		Map<String,Object> flowObjMap = (Map<String,Object>)JSONUtils.parseJSONFromString(exFlowJSON);
		
		ExecutableFlow parsedExFlow = ExecutableFlow.createExecutableFlowFromObject(flowObjMap);
		testEquals(exFlow, parsedExFlow);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testExecutorFlowUpdates() throws Exception {
		Flow flow = project.getFlow("jobe");
		ExecutableFlow exFlow = new ExecutableFlow(project, flow);
		exFlow.setExecutionId(101);
		
		// Create copy of flow
		Object obj = exFlow.toObject();
		String exFlowJSON = JSONUtils.toJSON(obj);
		@SuppressWarnings("unchecked")
		Map<String,Object> flowObjMap = (Map<String,Object>)JSONUtils.parseJSONFromString(exFlowJSON);
		ExecutableFlow copyFlow = ExecutableFlow.createExecutableFlowFromObject(flowObjMap);
		
		testEquals(exFlow, copyFlow);
		
		ExecutableNode joba = exFlow.getExecutableNode("joba");
		ExecutableFlowBase jobb = (ExecutableFlowBase)(exFlow.getExecutableNode("jobb"));
		ExecutableFlowBase jobc = (ExecutableFlowBase)(exFlow.getExecutableNode("jobc"));
		ExecutableFlowBase jobd = (ExecutableFlowBase)(exFlow.getExecutableNode("jobd"));
		ExecutableNode jobe = exFlow.getExecutableNode("jobe");
		assertNotNull(joba, jobb, jobc, jobd, jobe);
		
		ExecutableNode jobbInnerFlowA = jobb.getExecutableNode("innerJobA");
		ExecutableNode jobbInnerFlowB = jobb.getExecutableNode("innerJobB");
		ExecutableNode jobbInnerFlowC = jobb.getExecutableNode("innerJobC");
		ExecutableNode jobbInnerFlow = jobb.getExecutableNode("innerFlow");
		assertNotNull(jobbInnerFlowA, jobbInnerFlowB, jobbInnerFlowC, jobbInnerFlow);
		
		ExecutableNode jobcInnerFlowA = jobc.getExecutableNode("innerJobA");
		ExecutableNode jobcInnerFlowB = jobc.getExecutableNode("innerJobB");
		ExecutableNode jobcInnerFlowC = jobc.getExecutableNode("innerJobC");
		ExecutableNode jobcInnerFlow = jobc.getExecutableNode("innerFlow");
		assertNotNull(jobcInnerFlowA, jobcInnerFlowB, jobcInnerFlowC, jobcInnerFlow);
		
		ExecutableNode jobdInnerFlowA = jobd.getExecutableNode("innerJobA");
		ExecutableNode jobdInnerFlowB = jobd.getExecutableNode("innerJobB");
		ExecutableNode jobdInnerFlowC = jobd.getExecutableNode("innerJobC");
		ExecutableNode jobdInnerFlow = jobd.getExecutableNode("innerFlow");
		assertNotNull(jobdInnerFlowA, jobdInnerFlowB, jobdInnerFlowC, jobdInnerFlow);
		
		exFlow.setEndTime(1000);
		exFlow.setStartTime(500);
		exFlow.setStatus(Status.RUNNING);
		exFlow.setUpdateTime(133);
		
		// Change one job and see if it updates
		jobe.setEndTime(System.currentTimeMillis());
		jobe.setUpdateTime(System.currentTimeMillis());
		jobe.setStatus(Status.DISABLED);
		jobe.setStartTime(System.currentTimeMillis() - 1000);
		// Should be one node that was changed
		Map<String,Object> updateObject = exFlow.toUpdateObject(0);
		Assert.assertEquals(1, ((List)(updateObject.get("nodes"))).size());
		// Reapplying should give equal results.
		copyFlow.applyUpdateObject(updateObject);
		testEquals(exFlow, copyFlow);
		
		// This update shouldn't provide any results
		updateObject = exFlow.toUpdateObject(System.currentTimeMillis());
		Assert.assertNull(updateObject.get("nodes"));
		
		// Change inner flow
		jobbInnerFlowA.setEndTime(System.currentTimeMillis());
		jobbInnerFlowA.setUpdateTime(System.currentTimeMillis());
		jobbInnerFlowA.setStatus(Status.DISABLED);
		jobbInnerFlowA.setStartTime(System.currentTimeMillis() - 1000);
		// We should get 2 updates if we do a toUpdateObject using 0 as the start time
		updateObject = exFlow.toUpdateObject(0);
		Assert.assertEquals(2, ((List)(updateObject.get("nodes"))).size());

		// This should provide 1 update. That we can apply
		updateObject = exFlow.toUpdateObject(jobe.getUpdateTime());
		Assert.assertEquals(1, ((List)(updateObject.get("nodes"))).size());
		copyFlow.applyUpdateObject(updateObject);
		testEquals(exFlow, copyFlow);
		
		// This shouldn't give any results anymore
		updateObject = exFlow.toUpdateObject(jobbInnerFlowA.getUpdateTime());
		Assert.assertNull(updateObject.get("nodes"));
	}
	
	private void assertNotNull(ExecutableNode ... nodes) {
		for (ExecutableNode node: nodes) {
			Assert.assertNotNull(node);
		}
	}
	
	public static void testEquals(ExecutableNode a, ExecutableNode b) {
		if (a instanceof ExecutableFlow) {
			if (b instanceof ExecutableFlow) {
				ExecutableFlow exA = (ExecutableFlow)a;
				ExecutableFlow exB = (ExecutableFlow)b;
				
				Assert.assertEquals(exA.getScheduleId(), exB.getScheduleId());
				Assert.assertEquals(exA.getProjectId(), exB.getProjectId());
				Assert.assertEquals(exA.getVersion(), exB.getVersion());
				Assert.assertEquals(exA.getSubmitTime(), exB.getSubmitTime());
				Assert.assertEquals(exA.getSubmitUser(), exB.getSubmitUser());
				Assert.assertEquals(exA.getExecutionPath(), exB.getExecutionPath());
				
				testEquals(exA.getExecutionOptions(), exB.getExecutionOptions());
			}
			else {
				Assert.fail("A is ExecutableFlow, but B is not");
			}
		}

		if (a instanceof ExecutableFlowBase) {
			if (b instanceof ExecutableFlowBase) {
				ExecutableFlowBase exA = (ExecutableFlowBase)a;
				ExecutableFlowBase exB = (ExecutableFlowBase)b;
				
				Assert.assertEquals(exA.getFlowId(), exB.getFlowId());
				Assert.assertEquals(exA.getExecutableNodes().size(), exB.getExecutableNodes().size());
				
				for(ExecutableNode nodeA : exA.getExecutableNodes()) {
					ExecutableNode nodeB = exB.getExecutableNode(nodeA.getId());
					Assert.assertNotNull(nodeB);
					Assert.assertEquals(a, nodeA.getParentFlow());
					Assert.assertEquals(b, nodeB.getParentFlow());
					
					testEquals(nodeA, nodeB);
				}
			}
			else {
				Assert.fail("A is ExecutableFlowBase, but B is not");
			}
		}
		
		Assert.assertEquals(a.getId(), b.getId());
		Assert.assertEquals(a.getStatus(), b.getStatus());
		Assert.assertEquals(a.getStartTime(), b.getStartTime());
		Assert.assertEquals(a.getEndTime(), b.getEndTime());
		Assert.assertEquals(a.getUpdateTime(), b.getUpdateTime());
		Assert.assertEquals(a.getAttempt(), b.getAttempt());

		Assert.assertEquals(a.getJobSource(), b.getJobSource());
		Assert.assertEquals(a.getPropsSource(), b.getPropsSource());
		Assert.assertEquals(a.getInNodes(), a.getInNodes());
		Assert.assertEquals(a.getOutNodes(), a.getOutNodes());
	}
	
	
	
	public static void testEquals(ExecutionOptions optionsA, ExecutionOptions optionsB) {
		Assert.assertEquals(optionsA.getConcurrentOption(), optionsB.getConcurrentOption());
		Assert.assertEquals(optionsA.getNotifyOnFirstFailure(), optionsB.getNotifyOnFirstFailure());
		Assert.assertEquals(optionsA.getNotifyOnLastFailure(), optionsB.getNotifyOnLastFailure());
		Assert.assertEquals(optionsA.getFailureAction(), optionsB.getFailureAction());
		Assert.assertEquals(optionsA.getPipelineExecutionId(), optionsB.getPipelineExecutionId());
		Assert.assertEquals(optionsA.getPipelineLevel(), optionsB.getPipelineLevel());
		Assert.assertEquals(optionsA.isFailureEmailsOverridden(), optionsB.isFailureEmailsOverridden());
		Assert.assertEquals(optionsA.isSuccessEmailsOverridden(), optionsB.isSuccessEmailsOverridden());
		
		testEquals(optionsA.getDisabledJobs(), optionsB.getDisabledJobs());
		testEquals(optionsA.getSuccessEmails(), optionsB.getSuccessEmails());
		testEquals(optionsA.getFailureEmails(), optionsB.getFailureEmails());
		testEquals(optionsA.getFlowParameters(), optionsB.getFlowParameters());
	}
	
	public static void testEquals(Set<String> a, Set<String> b) {
		if (a == b) {
			return;
		}
		
		if (a == null || b == null) {
			Assert.fail();
		}
		
		Assert.assertEquals(a.size(), b.size());
		
		Iterator<String> iterA = a.iterator();
		
		while(iterA.hasNext()) {
			String aStr = iterA.next();
			Assert.assertTrue(b.contains(aStr));
		}
	}
	
	public static void testEquals(List<String> a, List<String> b) {
		if (a == b) {
			return;
		}
		
		if (a == null || b == null) {
			Assert.fail();
		}
		
		Assert.assertEquals(a.size(), b.size());
		
		Iterator<String> iterA = a.iterator();
		Iterator<String> iterB = b.iterator();
		
		while(iterA.hasNext()) {
			String aStr = iterA.next();
			String bStr = iterB.next();
			
			Assert.assertEquals(aStr, bStr);
		}
	}
	
	public static void testEquals(Map<String, String> a, Map<String, String> b) {
		if (a == b) {
			return;
		}
		
		if (a == null || b == null) {
			Assert.fail();
		}
		
		Assert.assertEquals(a.size(), b.size());
		
		for (String key: a.keySet()) {
			Assert.assertEquals(a.get(key), b.get(key));
		}
	}
}
