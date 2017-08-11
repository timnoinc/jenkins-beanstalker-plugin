package org.jenkinsci.plugins.beanstalker;

import java.io.IOException;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class EBVersion extends Builder implements SimpleBuildStep {

	private final String applicationName;
	private String versionLabel;
	private String includes;
	private String excludes;
//	private static AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
	private static AWSElasticBeanstalk eb = AWSElasticBeanstalkClientBuilder.defaultClient();;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public EBVersion(String applicationName, String versionLabel) {
		this.applicationName = applicationName;
		this.versionLabel = versionLabel;
	}

	@DataBoundSetter
	public void setVersionLabel(String VersionLabel) {
		this.versionLabel = VersionLabel;
	}

	public String getVersionLabel() {
		return this.versionLabel;
	}

	@DataBoundSetter
	public void setIncludes(String includes) {
		this.includes = includes;
	}

	public String getIncludes() {
		return this.includes;
	}

	@DataBoundSetter
	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}

	public String getExcludes() {
		return this.excludes;
	}

	/**
	 * We'll use this from the {@code config.jelly}.
	 */
	public String getApplicationName() {
		return applicationName;
	}

	 @Override
	 public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher,
	 TaskListener listener) {
	 // This is where you 'build' the project.
	 // Since this is a dummy, we just say 'hello world' and call that a build.
	
	 // This also shows how you can consult the global configuration of the
	 // builder
	
	 listener.getLogger().println("connect to " + applicationName +
	 "!"+versionLabel + includes+excludes);
	
	
	 }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		// This is where you 'build' the project.
		// Since this is a dummy, we just say 'hello world' and call that a build.

		// This also shows how you can consult the global configuration of the builder

		listener.getLogger().println("connect to " + applicationName + "!" + versionLabel + includes + excludes);

		return true;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link HelloWorldBuilder}. Used as a singleton. The class is
	 * marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See
	 * {@code src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly}
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Symbol("ebVersion")
	@Extension // This indicates to Jenkins that this is an implementation of an extension
				// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information, simply store it in a field and
		 * call save().
		 *
		 * <p>
		 * If you don't want fields to be persisted, use {@code transient}.
		 */
		private boolean useFrench;

		/**
		 * In order to load the persisted global configuration, you have to call load()
		 * in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 *         <p>
		 *         Note that returning {@link FormValidation#error(String)} does not
		 *         prevent the form from being saved. It just means that a message will
		 *         be displayed to the user.
		 */
		public FormValidation doCheckApplicationName(@QueryParameter String value)
				throws IOException, ServletException {
			
			if (value.length() == 0)
				return FormValidation.error("Please set an application name");
			if(eb.describeApplications(new DescribeApplicationsRequest().withApplicationNames(value)).getApplications().isEmpty()) {
				return FormValidation.error("Applciation not found");
			}
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Upload Elastic Beanstalk Application Version";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			useFrench = formData.getBoolean("useFrench");
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this, like
			// setUseFrench)
			save();
			return super.configure(req, formData);
		}

		/**
		 * This method returns true if the global configuration says we should speak
		 * French.
		 *
		 * The method name is bit awkward because global.jelly calls this method to
		 * determine the initial state of the checkbox by the naming convention.
		 */
		public boolean getUseFrench() {
			return useFrench;
		}
	}
}
