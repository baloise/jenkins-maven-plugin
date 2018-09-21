package com.baloise.maven.jenkins;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Ignore;
import org.junit.Test;

public class JenkinsRunnerTest {
	
	@Test
	public void testJre() throws Exception {
		File jre = new File(JenkinsRunner.jre());
		assertTrue(jre.exists());
		assertTrue(jre.canExecute());
	}
	
	@Test
	@Ignore
	public void runJenkins() throws Exception {
		String war = "target/jenkins-war-2.86.war";
		new JenkinsRunner().runJenkins(new File("target/JENKINS_HOME"), "/", 8081, war, null, new SystemStreamLog(), 5555);
	}

}
