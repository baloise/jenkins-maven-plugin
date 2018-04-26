package com.baloise.maven.jenkins;

import static java.io.File.separator;
import static java.util.regex.Pattern.quote;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Scanner;

import org.apache.maven.plugin.logging.Log;

public class JenkinsRunner {

	public static String jre() {
		RuntimeMXBean mxbean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
		String java = mxbean.getBootClassPath().split(quote(separator) + "lib", 2)[0] + separator + "bin" + separator + "javaw";
		return new File(java).exists() ? java : java + ".exe";
	}

	private Process proc;

	public void runJenkins(File jenkinsHome, String context, int port, String jenkinsWar, File jenkinsHomeTemplate, Log log) throws Exception {
		copyTemplate(jenkinsHomeTemplate, jenkinsHome);
		if(!context.isEmpty() &&  !context.startsWith("/")) context = "/"+context;
		addShutdownHook();
		new Thread() {{setDaemon(true);}
		public void run() {waitForExit();}
		}.start();
		System.out.println(String.format("Starting %s with home at %s as http://localhost:%s%s", jenkinsWar, jenkinsHome, port, context));
		ProcessBuilder pb = new ProcessBuilder(
				jre(), 
				"-Djenkins.install.runSetupWizard=false", 
				"-jar", 
				jenkinsWar,
				"--httpPort="+port,
				"--prefix="+context
				);
		pb.environment().put("JENKINS_HOME", jenkinsHome.toString());
		proc = pb.start();
		inheritIO(proc.getInputStream(), log, false);
		inheritIO(proc.getErrorStream(), log, true);
		proc.waitFor();
	}
	
	private static void inheritIO(final InputStream src, final Log  dest, final boolean err) {
	    new Thread(new Runnable() {
	        public void run() {
	            Scanner sc = new Scanner(src);
	            while (sc.hasNextLine()) {
	            	if(err) 
	            		dest.error(sc.nextLine());
	            	else
	            		dest.info(sc.nextLine());
	            }
	        }
	    }) {{setDaemon(true);}}.start();
	}

	private void copyTemplate(File jenkinsHomeTemplate, File jenkinsHome) throws IOException {
		if(jenkinsHomeTemplate == null) jenkinsHomeTemplate = detectJenkinsHomeTemplate();
		if(jenkinsHomeTemplate == null) return;
		System.out.println("copying resources from "+ jenkinsHomeTemplate + " to  "+jenkinsHome);
		FUtil.copyDirectory(jenkinsHomeTemplate, jenkinsHome);
	}

	private File detectJenkinsHomeTemplate() {
		File ret = new File("JENKINS-HOME-TEMPLATE");
		if(ret.exists()) return ret;
		ret = new File("src/test/resources/JENKINS-HOME-TEMPLATE");
		if(ret.exists()) return ret;
		ret = new File("src/test/resources/JENKINS-HOME");
		if(ret.exists()) return ret;
		return null;
	}

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					System.out.println("Shutting down JENKINS");
					proc.destroy();
				} catch (Exception e) {
				}
			}
		});
	}

	private static void waitForExit() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		printMessage();
		while (true) {
			try {
				if ("exit".equalsIgnoreCase(br.readLine())) {
					System.exit(0);
				} else {
					printMessage();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private static void printMessage() {
		System.out.flush();
		System.err.flush();
		System.out.println("To quit type 'exit'");
	}

}
