# jenkins-maven-plugin

A maven plugin to help develop and deploy JENKINS workflow libraries and pipelines.

The design goals are

- keep your POM clean: no dependencies
- keep your workspace clean : flexibilty in layout with good defaults
 
## Quick start

Prerequisite
- Maven is working. That's all. No pom.xml required.
- add jcenter repo to your maven settings

```<pluginRepository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray</id>
                    <name>bintray</name>
                    <url>http://jcenter.bintray.com</url>
</pluginRepository>```


lets go some where safe ...

`> mkdir /tmp/jenkins-test; cd /tmp/jenkins-test`

run jenkins ...

`> mvn com.baloise.maven:jenkins-maven-plugin:run`

go play at http://127.0.0.1:8080/

shut down via 

http://localhost:8080/exit

or type *exit* in the console (also works when launched in eclipse via m2 or external task)

## Make your life easier with plugin groups

Add the following to your *~/.m2/settings.xml*

```
<pluginGroups>
	<pluginGroup>com.baloise.maven</pluginGroup>
</pluginGroups>
```

now you can use

`> mvn jenkins:run`

[(tell me more)](http://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html#Configuring_Maven_to_Search_for_Plugins)

## Configuration options (TBD)

Of course you have all the options as where to set the properties


  @Parameter(defaultValue = "${project.build.directory}/JENKINS-HOME", property = "jenkins.home")
  private File home;
  
  @Parameter(defaultValue = "8080", property = "jenkins.port")
  int port;

  @Parameter(defaultValue = "", property = "jenkins.context")
  String context;
  
  @Parameter(property = "jenkins.war", required=false)
  String war = null;

[(tell me more)](http://docs.codehaus.org/display/MAVENUSER/MavenPropertiesGuide)


[![Build Status](https://travis-ci.org/baloise/jenkins-maven-plugin.svg)](https://travis-ci.org/baloise/jenkins-maven-plugin)
