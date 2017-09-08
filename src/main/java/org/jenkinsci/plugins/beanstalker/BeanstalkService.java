package org.jenkinsci.plugins.beanstalker;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public final class BeanstalkService {

	
	private AmazonS3 s3;
	private static AWSElasticBeanstalk eb;
	
	public BeanstalkService() {
		s3 = AmazonS3ClientBuilder.defaultClient();
		eb = AWSElasticBeanstalkClientBuilder.defaultClient();
	}
	
	public static AWSElasticBeanstalk get() {
		return eb;
	}
	public void createVersion() {
		eb.describeApplicationVersions();
		s3.listBuckets();
	}
	
	
	
}
