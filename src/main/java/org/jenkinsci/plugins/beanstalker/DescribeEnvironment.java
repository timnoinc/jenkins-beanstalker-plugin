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
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.trilead.ssh2.log.Logger;

import hudson.AbortException;
import hudson.Extension;
import hudson.Util;

public class DescribeEnvironment extends Step {

	private final String applicationName;
	private String environmentName;

	private static class AWSElasticBeanstalkHolder {
		private static final AWSElasticBeanstalk instance = AWSElasticBeanstalkClientBuilder.standard()
//				.withRegion("us-east-1")
				.build();
	}

	@DataBoundConstructor
	public DescribeEnvironment(String applicationName) {
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
			return "describeEBEnvironment";
		}

		@Override
		public String getDisplayName() {
			return "Beanstalker Describe Environment";
		}

		@Override
		public Set<Class<?>> getRequiredContext() {
			return Collections.emptySet();
		}

	}

	public static class Execution extends SynchronousNonBlockingStepExecution<EnvironmentDescription> {
		private transient final DescribeEnvironment step;
		private static final Logger LOGGER = Logger.getLogger(Execution.class);

		Execution(DescribeEnvironment step, StepContext context) {
			super(context);
			this.step = step;
		}

		@Override
		protected EnvironmentDescription run() throws Exception {
			DescribeEnvironmentsResult result = AWSElasticBeanstalkHolder.instance.describeEnvironments(new DescribeEnvironmentsRequest()
					.withApplicationName(step.getApplicationName())
					.withEnvironmentNames(step.getEnvironmentName())
					);
			
			List<EnvironmentDescription> e = result.getEnvironments();
			
			if (e.isEmpty()) {
//				ProcessBuilder pb = new ProcessBuilder("aws","elasticbeanstalk","describeEnvironments", "--ApplicationName" );
				throw new AbortException("Couldn't find any environments named "+ step.getEnvironmentName() +" in the application " + step.getApplicationName() + ": " + result.getSdkHttpMetadata().getHttpStatusCode() + result.getSdkHttpMetadata().getHttpHeaders()) ;
				
			}
//			if (e.size() > 1) {
//				throw new AbortException("Somehow got more than one environment in that application with that name?");
//			}
			LOGGER.log(0, "environments: " + e); 
			
			return e.get(0);
		}

		private static final long serialVersionUID = 1L;

	}

}
