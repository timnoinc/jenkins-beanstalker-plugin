package org.jenkinsci.plugins.beanstalker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

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
import hudson.util.DirScanner;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Zip up the workspace and load it as a new version on an Elastic Beanstalk
 * Application assumes that credentials are configured outside of this code.
 * 
 * @author Tim Rau
 *
 */
public class EBVersion extends Builder implements SimpleBuildStep {

	private final String applicationName;

	//
	private static class s3 { // initialization-on-demand holder idiom
		static final AmazonS3 instance = AmazonS3ClientBuilder.defaultClient();
	} // lazy initialization because eager init slows down the config page loading

	private static class eb { // initialization-on-demand holder idiom
		static final AWSElasticBeanstalk instance = AWSElasticBeanstalkClientBuilder.defaultClient();
	} // lazy initialization because eager init slows down the config page loading

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public EBVersion(String applicationName, String versionLabel) {
		this.applicationName = applicationName;
		this.versionLabel = versionLabel;
	}

	private @CheckForNull String versionLabel;

	@DataBoundSetter
	public void setVersionLabel(@CheckForNull String VersionLabel) {
		this.versionLabel = VersionLabel;
	}

	public @CheckForNull String getVersionLabel() {
		return this.versionLabel;
	}

	private @CheckForNull String includes;
	
	@DataBoundSetter
	public void setIncludes(@Nonnull String includes) {
		this.includes = includes.equals(DescriptorImpl.defaultIncludes) ? null : includes;
	}

	public @Nonnull String getIncludes() {
		return this.includes == null ? DescriptorImpl.defaultIncludes : this.includes;
	}

	private @CheckForNull String excludes;

	@DataBoundSetter
	public void setExcludes(@CheckForNull String excludes) {
		this.excludes = excludes;
	}

	public @CheckForNull String getExcludes() {
		return this.excludes;
	}
	
	private @CheckForNull String s3Bucket;
	@DataBoundSetter
	public void setS3Bucket(@CheckForNull String s3Bucket) {
		this.s3Bucket = s3Bucket;
	}
	public @CheckForNull String getS3Bucket() {
		return this.s3Bucket;
	}
	
	private @CheckForNull String s3Prefix;
	@DataBoundSetter
	public void setS3Prefix(@CheckForNull String s3Prefix) {
		this.s3Prefix = s3Prefix;
	}
	public @CheckForNull String getS3Prefix() {
		return this.s3Prefix;
	}
	

	/**
	 * We'll use this from the {@code config.jelly}.
	 */
	public String getApplicationName() {
		return applicationName;
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
		// This is where you 'build' the project.
		// Since this is a dummy, we just say 'hello world' and call that a build.

		// This also shows how you can consult the global configuration of the
		// builder

		listener.getLogger().println("connect to " + applicationName + "!" + versionLabel + includes + excludes);

	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		// This is where you 'build' the project.
		// Since this is a dummy, we just say 'hello world' and call that a build.

		// This also shows how you can consult the global configuration of the builder
		
		File zipFile = File.createTempFile("awseb-", ".zip");
		
		// maybe later allow configurable zip root
		FilePath targetPath = build.getWorkspace();
		if (targetPath == null) {
			throw new RuntimeException("couldn't find workspace");
		}

		listener.getLogger().println("Zipping contents of "+ targetPath.getName() +" into " + zipFile.getPath() + " (includes="+includes+", excludes="+excludes+")");
		FileOutputStream os = new FileOutputStream(zipFile);
		try {
		targetPath.zip(os, new DirScanner.Glob(includes, excludes));
		} finally {
			os.close();
		}
		
		String s3Key = s3Prefix + versionLabel+".zip";
		listener.getLogger().printf("Uploading %s  to s3 Bucket %s as %s",zipFile.getPath(),s3Bucket,s3Key);
		s3.instance.putObject(s3Bucket, s3Key, zipFile);
		
		
		S3Location location = new S3Location(s3Bucket, s3Key);
		listener.getLogger().printf("Creating new Version '%s' in Application %s from %s", versionLabel,applicationName,location);
		eb.instance.createApplicationVersion(new CreateApplicationVersionRequest(applicationName, versionLabel).withSourceBundle(location));

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
		public static final String defaultIncludes = "**/*";

		/**
		 * In order to load the persisted global configuration, you have to call load()
		 * in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'ApplicationName'.
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
			if (eb.instance.describeApplications(new DescribeApplicationsRequest().withApplicationNames(value))
					.getApplications().isEmpty()) {
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
			// useFrench = formData.getBoolean("useFrench");
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this, like
			// setUseFrench)
			save();
			return super.configure(req, formData);
		}

	}
}
