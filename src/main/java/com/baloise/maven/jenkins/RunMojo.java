package com.baloise.maven.jenkins;

import static java.lang.String.format;

import java.io.File;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, requiresProject = false)
public class RunMojo extends AbstractMojo {

	static final String JENKINS_GROUP_ID = "org.jenkins-ci.main";
	static final String JENKINS_ARTIFACT_ID = "jenkins-war";

	@Parameter(defaultValue = "8080", property = "jenkins.port", required = true)
	int port;

	@Parameter(defaultValue = "", property = "jenkins.context", required = false)
	String context;

	@Parameter(property = "jenkins.war", required = false)
	String war = null;

	@Parameter(property = "jenkins.home.template", required = false)
	File jenkinsHomeTemplate = null;

	@Parameter(defaultValue = "2.86", property = "jenkins.version", required = false)
	String version = null;

	@Component
	private Settings settings;

	@Component
	private PluginDescriptor plugin;

	@Component
	private RepositorySystem repoSystem;

	@Parameter(defaultValue = "${repositorySystemSession}")
	private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}")
	private List<RemoteRepository> projectRepos;

	@Parameter(defaultValue = "${project.build.directory}")
	protected File private__buildDir;

	@Parameter(property = "jenkins.home")
	protected File home;

	protected void doExecute() throws MojoExecutionException, MojoFailureException {
		adjustContext();
		getLog().info("jenkins.port: " + port);
		getLog().info("jenkins.context: " + context);
		getLog().info("jenkins.version: " + version);
		if (war == null)
			war = resolve(JENKINS_GROUP_ID + ":" + JENKINS_ARTIFACT_ID + ":war:" + version).getArtifact().getFile()
					.getAbsolutePath();
		getLog().info("jenkins.war: " + war);
		getLog().info(format("Starting JENKINS server at http://%s:%s%s", getHostName(), port, context));
		try {
			new JenkinsRunner().runJenkins(home, context, port, war, jenkinsHomeTemplate);
		} catch (Exception e) {
			getLog().error(e);
		}
	}

	public void adjustContext() {
		if (context == null)
			context = "";
		context = context.trim();
		if (context.length() > 0 && context.charAt(0) != '/')
			context = "/" + context;
	}

	private String getHostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}

	public ArtifactResult resolve(String artifactCoords) throws MojoExecutionException, MojoFailureException {
		Artifact artifact;
		try {
			artifact = new DefaultArtifact(artifactCoords);
		} catch (IllegalArgumentException e) {
			throw new MojoFailureException(e.getMessage(), e);
		}

		ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(artifact);
		request.setRepositories(projectRepos);

		getLog().info("Resolving artifact " + artifact + " from " + projectRepos);

		ArtifactResult result;
		try {
			result = repoSystem.resolveArtifact(repoSession, request);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		getLog().info("Resolved artifact " + artifact + " to " + result.getArtifact().getFile() + " from "
				+ result.getRepository());
		return result;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		adjustHome();
		getLog().info("jenkins.home: " + home.getAbsolutePath());
		doExecute();
	}

	private void adjustHome() {
		if (home == null) {
			home = hasPom() ? new File(private__buildDir, "JENKINS-HOME") : new File("JENKINS-HOME");
		}
	}

	private boolean hasPom() {
		return new File("pom.xml").exists();
	}

}
