package org.jenkinsci.plugins.beanstalker;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.slaves.WorkspaceList;

public class EBGetVersion extends Step {

	private final String applicationName;
	private String environmentName;

	private static class AWSElasticBeanstalkHolder {
		private static final AWSElasticBeanstalk instance = AWSElasticBeanstalkClientBuilder.defaultClient();
	}

	@DataBoundConstructor
	public EBGetVersion(String applicationName) {
		this.applicationName = applicationName;
	}

	@CheckForNull
	public String getApplicationName() {
		return this.applicationName;
	}

	@DataBoundSetter
	public void setEnvironmentName(@CheckForNull String environmentName) {
		this.environmentName = Util.fixEmpty(environmentName);
	}

	@CheckForNull
	public String getEnvironmentName() {
		return this.environmentName;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		// TODO Auto-generated method stub
		return new Execution(this, context);
	}

	@Extension
	public static final class DescriptorImpl extends StepDescriptor {

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

		@Override
		public Set<Class<?>> getRequiredContext() {
			return Collections.emptySet();
		}

	}

	public static class Execution extends SynchronousNonBlockingStepExecution<String> {
		private EBGetVersion step;

		Execution(EBGetVersion step, StepContext context) {
			super(context);
			this.step = step;
		}

		@Override
		protected String run() throws Exception {
			List<EnvironmentDescription> e = AWSElasticBeanstalkHolder.instance.describeEnvironments(new DescribeEnvironmentsRequest()
					.withApplicationName(step.getApplicationName())
					.withEnvironmentIds(step.getEnvironmentName())).getEnvironments();
			if (e.isEmpty()) {
				throw new AbortException("Couldn't find any environments with that name!");
			}
			if (e.size() > 1) {
				throw new AbortException("Somehow got more than one environment in that application with that name?");
			}
			return e.get(0).getVersionLabel();
		}

		private static final long serialVersionUID = 1L;

	}

}
