package com.connexta.libero

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.exception.GrgitException
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationResult
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.Invoker

import java.text.SimpleDateFormat

// TODO: What should be the process when the build fails? How should the fixes be handled?
//       should that be part of the script? Should the script have an option to only do a build?
// TODO: Add option to clone the repository
// TODO: Check if remote is a url or just a name
// TODO: add option to clean artifacts from local .m2
// TODO: add verification step for git ref
//  see git rev-parse for validation

class Libero {

    private final Util util = new Util()
    private final Invoker maven = new DefaultInvoker()
    private final Properties quickBuildProps = [
            "findbugs.skip"  : "true",
            "pmd.skip"       : "true",
            "jacoco.skip"    : "true",
            skipTestScope    : "true",
            skipProvidedScope: "true",
            skipRuntimeScope : "true",
            skipTests        : "true"
    ]

    /**
     * Computes all derived configuration values and replaces any property usages.
     * @param config
     */
    public void computeProperties(Config config) {

        // Check that projectDir is set and exists
        if (!config.projectDir && !(new File(config.projectDir).exists())) {
            throw new RuntimeException("Project Directory: ${config.projectDir} is invalid")
        }

        Grgit git = Grgit.open(dir: config.projectDir)

        config.originalBranch = git.branch.current.name

        initializeConfig(config)
        initialRepoPreparation(config, git)
        finalizeConfig(config)

        Date date = new Date()
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'")
        config.projectProperties.date = date
        config.projectProperties.timestamp = dateFormat.format(date)

        config.resolveProperties()
        git.close()
        config.projectProperties.computed = true
    }

    /**
     * Starts the release process
     * @param options
     * @param config
     */
    public void run(Options options, Config config) {
        if (!config.projectProperties.computed) {
            computeProperties(config)
        }

        Grgit git = Grgit.open(dir: config.projectDir)
        executeRelease(options, config, git)

        git.checkout(branch: config.originalBranch)
    }

    /**
     * Checks config file for missing values prior to checkout
     */
    private void initializeConfig(Config config) {
        config.projectName = config.projectName ?: new File(config.projectDir).getName()
        config.sourceRemote = config.sourceRemote ?: 'origin'
        config.destRemote = config.destRemote ?: config.sourceRemote
        config.ref = config.ref ?: 'master'
        config.destBranch = config.destBranch ?: 'master'
        config.preProps = config.preProps ?: [:]
        config.postProps = config.postProps ?: [:]
    }

    /**
     * performs initial preparation of git repository
     */
    private void initialRepoPreparation(Config config, Grgit git) {
        git.fetch(remote: config.sourceRemote)
        git.checkout(branch: config.ref)
        git.pull(remote: config.sourceRemote)
    }

    /**
     * Checks config file for missing values that require computed defaults
     */
    private void finalizeConfig(Config config) {
        config.startVersion = util.getPomVersion(new File(config.projectDir, "pom.xml"))
        config.projectProperties.baseVersion = util.getBaseVersion(config.startVersion)
        config.releaseVersion = config.releaseVersion ?: util.getBaseVersion(config.startVersion)
        config.nextVersion = config.nextVersion ?: util.incrementVersion(config.releaseVersion)
        config.releaseName = config.releaseName ?: "${config.projectName}-${config.releaseVersion}"
    }

    /**
     * Executes the release process
     * @return
     */
    private void executeRelease(Options options, Config config, Grgit git) {
        prepareRelease(config, git)
        runBuild(options, config, git)
    }

    /**
     * Prepares the release by updating pom versions and creating git tags
     */
    private void prepareRelease(Config config, Grgit git) {
        InvocationResult mavenResult
        InvocationRequest releaseVersionRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['versions:set'],
                properties: [newVersion: config.releaseVersion, generateBackupPoms: "false"])
        InvocationRequest devVersionRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['versions:set'],
                properties: [newVersion: config.nextVersion, generateBackupPoms: "false"])

        if (config.mavenSettings) {
            releaseVersionRequest.setUserSettingsFile(config.mavenSettings)
            devVersionRequest.setUserSettingsFile(config.mavenSettings)
        }

        mavenResult = maven.execute(releaseVersionRequest)
        if (mavenResult.getExitCode() != 0) {
          throw new IllegalStateException( "Failed to update the pom version from ${config.startVersion} to ${config.releaseVersion}" );
        }
        // Pre-Release property updates
        updateProps(config.preProps, config.projectDir)
        git.commit(message: "[libero] prepare release ${config.releaseName}", all: true)
        try {
            git.tag.add(name: config.releaseName)
        } catch (GrgitException e) {
            throw new IllegalStateException("Tag: ${config.releaseName} already exists. " +
                    "Either this version has already been released, or it failed to complete before")
        }

        // create dev version
        mavenResult = maven.execute(devVersionRequest)
        if (mavenResult.getExitCode() != 0) {
            throw new IllegalStateException( "Failed to update the pom version from ${config.releaseVersion} to ${config.nextVersion}" );
        }
        // Post-Release property updates
        updateProps(config.postProps, config.projectDir)
        git.commit(message: "[libero] prepare for next development iteration", all: true)
    }
/**
     * Executes the build and deploy of the release
     */
    private void runBuild(Options options, Config config, Grgit git) {
        InvocationResult mavenResult
        InvocationRequest buildRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['clean', 'install'])
        if (options.quickBuild) {
            buildRequest.setProperties(quickBuildProps)
        }
        InvocationRequest deployRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['deploy'],
                properties: quickBuildProps)

        if (config.mavenSettings) {
            buildRequest.setUserSettingsFile(config.mavenSettings)
            deployRequest.setUserSettingsFile(config.mavenSettings)
        }

        // Check out the git tag
        git.checkout(branch: config.releaseName)
        mavenResult = maven.execute(buildRequest)
        if (mavenResult.getExitCode() != 0) {
            throw new IllegalStateException( "Release Build failed!!" );
        }
        // TODO: dry run should either remove tags and reset, or provide advice on how to do so
        if (!options.dryRun) {
            if (config.mavenRepo) {
                deployRequest.setProperties((quickBuildProps + [altDeploymentRepository: config.mavenRepo]) as Properties)
            }
            mavenResult = maven.execute(deployRequest)
            if (mavenResult.getExitCode() != 0) {
                throw new IllegalStateException( "Release deploy failed!!" );
            }
            if (options.gitPush) {
                git.push(remote: config.destRemote, tags: true, refsOrSpecs: [config.destBranch])
            }
        }

        git.checkout(branch: config.originalBranch)

        println "Release Process Complete!!"
    }

    private void updateProps(Map props, String directory) {
        File pom = new File(directory, "pom.xml")
        def xml = new XmlParser().parse(pom)
        props.each { k, v ->
            if (xml.properties."${k}" != null) {
                Node node = xml.properties."${k}"[0]
                node.setValue("${v}")
            }
            XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(new FileWriter(pom)))
            printer.preserveWhitespace = true
            printer.print(xml)
        }
    }
}
