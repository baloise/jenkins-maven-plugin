package com.baloise.maven.jenkins;

import static com.baloise.maven.jenkins.FUtil.copyDirectory;
import static java.io.File.separator;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.quote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.plugin.logging.Log;

public class JenkinsRunner {

	public static final String KILL_PID_PROPERTY = "jenkinsRunner.killPID";
	private static String lazyJre;
	public static String jre() {
		if(lazyJre != null) return lazyJre;
		RuntimeMXBean mxbean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
		List<String> names = asList("javaw", "java", "javaw.exe", "java.exe");
		
		if(mxbean.isBootClassPathSupported()) {
			String jdk = mxbean.getBootClassPath().split(quote(separator)+"lib"+quote(separator)+"\\w+\\.jar",2)[0];
			File bin = new File(jdk,  "bin");
			for(String fileName : names) {
				File ret = new File(bin, fileName);
				if(ret.exists()) { 
					lazyJre =  ret.getAbsolutePath();
					return lazyJre;
				}
			}
		}
		
		String libraryPath = mxbean.getLibraryPath();
		for(String bin : libraryPath.split(quote(File.pathSeparator))) {
			for(String fileName :names) {
				File ret = new File(bin, fileName);
				if(ret.exists()) {
					lazyJre =  ret.getAbsolutePath();
					return lazyJre;
				}
			}
		}
		
		String jdk = System.getProperty("java.home");
		if(jdk != null) {
			File bin = new File(jdk,  "bin");
			for(String fileName : names) {
				File ret = new File(bin, fileName);
				if(ret.exists()) { 
					lazyJre =  ret.getAbsolutePath();
					return lazyJre;
				}
			}
		}
		
		throw new IllegalStateException("java excutable not found");
	}

	private Process proc;
	private boolean restart = true;

	public void runJenkins(File jenkinsHome, String context, int port, String jenkinsWar, File jenkinsHomeTemplate, Log log) throws Exception {
		runJenkins(jenkinsHome, context, port, jenkinsWar, jenkinsHomeTemplate, log, -1);
	}
	
	public void runJenkins(File jenkinsHome, String context, int port, String jenkinsWar, File jenkinsHomeTemplate, Log log, int debugPort) throws Exception {
		checkFileExists(new File(jenkinsWar));
		copyTemplate(jenkinsHomeTemplate, jenkinsHome);
		if(!context.isEmpty() &&  !context.startsWith("/")) context = "/"+context;
		addShutdownHook();
		new Thread() {{setDaemon(true);}
		public void run() {waitForExit();}
		}.start();
		while(restart) {
			restart = false;
			System.out.println(String.format("Starting %s with home at %s as http://localhost:%s%s", jenkinsWar, jenkinsHome, port, context));
			List<String> command = new ArrayList<String>();
			command.add(jre());
			command.add("-Djenkins.install.runSetupWizard=false"); 
			if(debugPort > 0 ) {
				command.add("-Xdebug"); 
				command.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5555"); 
			}
			command.add("-jar"); 
			command.add(jenkinsWar);
			command.add("--enable-future-java");
			command.add("--httpPort="+port);
			command.add("--prefix="+context);
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.environment().put("JENKINS_HOME", jenkinsHome.toString());
			proc = pb.start();
			inheritIO(proc.getInputStream(), log, false);
			inheritIO(proc.getErrorStream(), log, true);
			proc.waitFor();
		}
	}

	private void checkFileExists(File file) throws FileNotFoundException {
		if(!file.exists()) throw new FileNotFoundException(file.getAbsolutePath()+" not found");
	}
	
	private static void inheritIO(final InputStream src, final Log  dest, final boolean err) {
	    new Thread(new Runnable() {
	        public void run() {
	            try (Scanner sc = new Scanner(src)) {
					while (sc.hasNextLine()) {
						if(err) 
							dest.error(sc.nextLine());
						else
							dest.info(sc.nextLine());
					}
				}
	        }
	    }) {{setDaemon(true);}}.start();
	}

	private void copyTemplate(File jenkinsHomeTemplate, File jenkinsHome) throws IOException {
		if(jenkinsHomeTemplate == null) jenkinsHomeTemplate = detectJenkinsHomeTemplate();
		if(jenkinsHomeTemplate == null) return;
		System.out.println("copying resources from "+ jenkinsHomeTemplate + " to  "+jenkinsHome);
		File initGroovyD = new File(jenkinsHome, "init.groovy.d");
		if(initGroovyD.exists()) {
			System.out.println("cleaning " +initGroovyD.getAbsolutePath());
			stream(initGroovyD.listFiles()).filter(File::isFile).forEach(File::delete);
		}
		copyDirectory(jenkinsHomeTemplate, jenkinsHome);
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
					killProcesses();
					proc.destroy();
				} catch (Exception e) {
				}
			}
		});
	}

	private void waitForExit() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		printMessage();
		while (true) {
			try {
				switch (br.readLine().toLowerCase()) {
					case "exit":
						System.exit(0);
						break;
					case "restart":
						System.out.println("Restarting JENKINS");
						restart = true;
						killProcesses();
						proc.destroy();
						break;
					default:
						printMessage();
						break;
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private void printMessage() {
		System.out.flush();
		System.err.flush();
		System.out.println("type 'restart' or 'exit'");
	}

	public static void addPIDtoKill(long pid) {
		System.setProperty(KILL_PID_PROPERTY, (System.getProperty(KILL_PID_PROPERTY, "") + " "+pid).trim());
	}
	
	private static long[] resetPIDtoKill() {
			String PIDs = System.getProperty(KILL_PID_PROPERTY, "");
			System.setProperty(KILL_PID_PROPERTY,"");
			return PIDs.isEmpty() ? new long[0] : stream(PIDs.split(" ")).mapToLong(Long::valueOf).toArray();
	}
	
	private static void killProcesses() {
		for (long pid : resetPIDtoKill()) {
			System.out.println("killing process with id "+pid);
			ProcessHandle.of(pid).ifPresent(processHandle -> processHandle.destroy());
		}
	}
	
	

}
