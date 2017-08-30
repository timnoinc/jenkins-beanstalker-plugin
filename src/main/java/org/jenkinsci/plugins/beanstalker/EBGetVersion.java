package org.jenkinsci.plugins.beanstalker;

import java.util.Collections;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static hudson.model.Result.ABORTED;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Executor;
import hudson.slaves.WorkspaceList;

public class EBGetVersion extends Step {
	
	private final String applicationName;
	private String environmentName;
	
	@DataBoundConstructor
	public EBGetVersion(String applicationName) {
		this.applicationName = applicationName;
	}
	
	@CheckForNull public String getApplicationName() {
		return this.applicationName;
	}
	
	@DataBoundSetter
	public void setEnvironmentName(@CheckForNull String environmentName) {
		this.environmentName = Util.fixEmpty(environmentName);
	}
	
	@CheckForNull public String getEnvironmentName() {
		return this.environmentName;
	}
	
	@Override
	public StepExecution start(StepContext context) throws Exception {
		// TODO Auto-generated method stub
		return new Execution(false, context);
	}

	@Extension
	public static final class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<Class<?>> getRequiredContext() {
//			return Collections.singleton(FilePath.class);
			return null;
		}

		@Override
		public String getFunctionName() {
			// TODO Auto-generated method stub
			return "EBGetVersion";
		}

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return "Beanstalker Get Version";
		}

	}

	// TODO use 1.652 use WorkspaceList.tempDir
	private static FilePath tempDir(FilePath ws) {
		return ws.sibling(ws.getName() + System.getProperty(WorkspaceList.class.getName(), "@") + "tmp");
	}

	public static class Execution extends StepExecution {
		private transient volatile Thread executing;
		@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Only used when starting.")
		private transient final boolean tmp;

		Execution(boolean tmp, StepContext context) {
			super(context);
			this.tmp = tmp;
		}

		// @Override
		protected String run() throws Exception {
			FilePath cwd = getContext().get(FilePath.class);
			return (tmp ? tempDir(cwd) : cwd).getRemote();
		}

		private static final long serialVersionUID = 1L;

		@Override
		public boolean start() throws Exception {
			executing = Thread.currentThread();
			try {
				getContext().onSuccess(run());
			} catch (Throwable t) {
				getContext().onFailure(t);
			} finally {
				executing = null;
			}
			return true;
		}

		@Override
		public void stop(Throwable arg0) throws Exception {
			Thread e = executing; // capture
			if (e != null) {
				if (e instanceof Executor) {
					((Executor) e).interrupt(ABORTED);
				} else {
					e.interrupt();
				}
			}
		}

	}

}
