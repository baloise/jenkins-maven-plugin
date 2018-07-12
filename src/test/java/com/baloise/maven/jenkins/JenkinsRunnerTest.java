package com.baloise.maven.jenkins;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;

public class JenkinsRunnerTest {
	
	@Test
	public void testJre() throws Exception {
		File jre = new File(JenkinsRunner.jre());
		assertTrue(jre.exists());
		assertTrue(jre.canExecute());
	}

}
