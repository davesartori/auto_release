### NAB Release Plugin


## About
Allows you to perfrom release builds from within Jenkins.
To enable, once the plugin is installed in Jenkins, go to each projects' configure page (for the given build, usually develop). Select the checkbox against "Auto release build".

## Service Engine Setup
Create a gradle.properties file that contains the following:
versionNumber=1.3.0

Add gradle-auto-release plugin to your service engine build.gradle
buildscript {
  dependencies {
    classpath 'commons-configuration:commons-configuration:1.10'
    classpath "com.nab:gradle-auto-release:1.0.1-4"
    classpath "org.eclipse.jgit:org.eclipse.jgit:4.3.1.201605051710-r"

