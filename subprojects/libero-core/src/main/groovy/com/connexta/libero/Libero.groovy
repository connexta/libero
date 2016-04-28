package com.connexta.libero

import org.ajoberstar.grgit.Grgit
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.Invoker

import java.text.SimpleDateFormat

import static com.connexta.libero.Util.printConfig

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

    public void run(Options options, Config config) {

        // Check that projectDir is set and exists
        if (!config.projectDir && !(new File(config.projectDir).exists())) {
            println "Project Directory: ${config.projectDir} is invalid"
            System.exit(1)
        }

        Grgit git = Grgit.open(dir: config.projectDir)

        config.originalBranch = git.branch.current.name

        initializeConfig(config)
        initialRepoPreparation(config, git)
        finalizeConfig(config)

        Date date = new Date()
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        config.projectProperties.date = date
        config.projectProperties.timestamp = dateFormat.format(date)

        // Replace property references prior to running
        config.resolveProperties()

        if (!options.force) {

            println "Preparing for Release Cycle!"
            printConfig(options, config)

            //TODO: should probably move this to the cli class
            def USER_CONFIRMED = System.console().readLine 'Do you wish to continue? [y/N]: ' as String
            switch (USER_CONFIRMED) {
                case "n":
                    System.exit(1)
                    break
                case "y":
                    executeRelease(options, config, git)
                    break
                default:
                    println "Response must be either [y/n]"
                    System.exit(1)
            }
        } else {
            executeRelease(options, config, git)
        }
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
        InvocationRequest releaseVersionRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['versions:set'],
                properties: [newVersion: config.releaseVersion, generateBackupPoms: "false"])
        InvocationRequest devVersionRequest = new DefaultInvocationRequest(
                pomFile: new File(config.projectDir, "pom.xml"),
                goals: ['versions:set'],
                properties: [newVersion: config.nextVersion, generateBackupPoms: "false"])

        // Check if tag exists already
        List tags = git.tag.list()
        if (tags.contains(config.releaseName)) {
            println "Tag: ${config.releaseName} already exists. Either this version has already been released, or it failed to complete before"
            System.exit(1)
        }
        maven.execute(releaseVersionRequest)
        // Pre-Release property updates
        config.preProps.each { k, v ->
            util.executeCommand("sed -i '' \"s|<${k}>[^<>]*</${k}>|<${k}>${v}</${k}>|g\" pom.xml", config.projectDir)
        }
        git.commit(message: "[libero] prepare release ${config.releaseName}", all: true)
        git.tag.add(name: config.releaseName)

        // create dev version
        maven.execute(devVersionRequest)
        // Post-Release property updates
        config.postProps.each { k, v ->
            util.executeCommand("sed -i '' \"s|<${k}>[^<>]*</${k}>|<${k}>${v}</${k}>|g\" pom.xml", config.projectDir)
        }
        git.commit(message: "[libero] prepare for next development iteration", all: true)
    }

    /**
     * Executes the build and deploy of the release
     */
    private void runBuild(Options options, Config config, Grgit git) {

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

        // Check out the git tag
        git.checkout(branch: config.releaseName)
        maven.execute(buildRequest)

        // TODO: dry run should either remove tags and reset, or provide advice on how to do so
        if (!options.dryRun) {
            if (config.mavenRepo) {
                deployRequest.setProperties((quickBuildProps + [altDeploymentRepository: config.mavenRepo]) as Properties)
            }
            maven.execute(deployRequest)
            if (options.gitPush) {
                git.push(remote: config.destRemote, tags: true, refsOrSpecs: [config.destBranch])
            }
        }

        git.checkout(branch: config.originalBranch)

        println "Release Process Complete!!"
    }
}
