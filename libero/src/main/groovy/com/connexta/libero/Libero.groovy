package com.connexta.libero

import java.text.SimpleDateFormat

// TODO: What should be the process when the build fails? How should the fixes be handled?
//       should that be part of the script? Should the script have an option to only do a build?
// TODO: Add check for existing tag
// TODO: Add option to clone the repository
// TODO: Check if remote is a url or just a name
// TODO: add option to clean artifacts from local .m2
// TODO: add user prompts to get better information when user provides bad input (maybe)
// TODO: add verification step for git ref
//  see git rev-parse for validation
// TODO: change currentVersion finder to use git show rev:file to get the xml content

class Libero {

    Util util

    Libero() {
        this.util = new Util()
    }

    def run(Config config) {
        Date date = new Date()
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        config.projectProperties.date = date
        config.projectProperties.timestamp = dateFormat.format(date)
        config.resolveProperties()
        if (!config.force) {

            println "Preparing for Release Cycle!"
            config.printConfig()

            def USER_CONFIRMED = System.console().readLine 'Do you wish to continue? [y/N]: ' as String
            switch (USER_CONFIRMED) {
                case "n":
                    System.exit(1)
                    break
                case "y":
                    executeRelease(config)
                    break
                default:
                    println "Response must be either [y/n]"
                    System.exit(1)
            }
        } else {
            executeRelease(config)
        }
    }

    /**
     * Executes the build and deploy of the release
     * @param config Libero configuration
     */
    private def runBuild(Config config) {
        String gitCommand = "git"
        String gitCheckoutCommand = "${gitCommand} checkout ${config.releaseName}"
        String gitPushCommand = "${gitCommand} push ${config.destRemote}"
        String gitMasterCommand = "${gitCommand} checkout master"
        String mavenCommand = "mvn -f ${config.projectDir + "/pom.xml"}"
        String quickBuildParams = "-Dfindbugs.skip=true -DskipTests=true -Dpmd.skip=true -Djacoco.skip=true -DskipTestScope=true -DskipProvidedScope=true -DskipRuntimeScope=true"
        String deployCommand = "${mavenCommand} ${quickBuildParams} -T2C deploy"
        String buildCommand

        // Check out the git tag
        util.executeCommand(gitCheckoutCommand, config.projectDir)

        if (config.quickBuild) {
            buildCommand = "${mavenCommand} ${quickBuildParams} clean install"
        } else {
            buildCommand = "${mavenCommand} clean install"
        }
        util.executeCommand(buildCommand, config.projectDir)

        if (!config.dryRun) {
            if (config.mavenRepo) {
                deployCommand = "${deployCommand} -DaltDeploymentRepository=${config.mavenRepo}"
            }
            util.executeCommand(deployCommand, config.projectDir)
            if (config.push) {
                util.executeCommand(gitPushCommand, config.projectDir)
            }
        }

        // TODO: change to checkout original branch
        util.executeCommand(gitMasterCommand, config.projectDir)

        println "Release Process Complete!!"
    }

    /**
     * Executes the release process
     * @param config Libero Configuration
     * @return
     */
    private def executeRelease(Config config) {
        prepareRelease(config)
        runBuild(config)
    }

    /**
     * Prepares the release by updating pom versions and creating git tags
     * @param config Libero config
     * @return
     */
    private def prepareRelease(Config config) {

        // Define Commands
        String gitCommand = "git"
        String mavenCommand = "mvn"
        config.releaseName = "${config.projectName}-${config.releaseVersion}"
        String checkoutCommand = "${gitCommand} checkout ${config.ref}"
        String setReleaseVersionCommand = "${mavenCommand} versions:set -DnewVersion=${config.releaseVersion}"
        //TODO: check for alternatives to use-release (look at maven enforcer)
        // String updateSnapshotsCommand = "${mavenCommand} versions:use-releases -DincludesList=${config.nextSnapshotsFilter}"
        String mavenCommitVersionCommand = "${mavenCommand} versions:commit"
        String gitAddCommand = "${gitCommand} add ."
        String gitCommitReleaseCommand = "${gitCommand} commit -m \"[libero] prepare release ${config.releaseName}\""
        String gitTagCommand = "${gitCommand} tag ${config.releaseName}"
        String setDevVersionCommand = "${mavenCommand} versions:set -DnewVersion=${config.nextVersion}"
        // String nextSnapshotsCommand = "${mavenCommand} versions:use-next-snapshots -DincludesList=${config.nextSnapshotsFilter}"
        String gitCommitDevCommand = "${gitCommand} commit -m \"[libero] prepare for next development iteration\""


        util.executeCommand(checkoutCommand, config.projectDir)
        util.executeCommand(setReleaseVersionCommand, config.projectDir)
        util.executeCommand(mavenCommitVersionCommand, config.projectDir)

        // Pre-Release property updates
        config.preProps.each { k, v ->
            util.executeCommand("sed -i '' \"s|<${k}>[^<>]*</${k}>|<${k}>${v}</${k}>|g\" pom.xml", config.projectDir)
        }
        util.executeCommand(gitAddCommand, config.projectDir)
        util.executeCommand(gitCommitReleaseCommand, config.projectDir)
        util.executeCommand(gitTagCommand, config.projectDir)
        util.executeCommand(setDevVersionCommand, config.projectDir)
        util.executeCommand(mavenCommitVersionCommand, config.projectDir)
        // Post-Release property updates
        config.postProps.each { k, v ->
            util.executeCommand("sed -i '' \"s|<${k}>[^<>]*</${k}>|<${k}>${v}</${k}>|g\" pom.xml", config.projectDir)
        }
        util.executeCommand(gitAddCommand, config.projectDir)
        util.executeCommand(gitCommitDevCommand, config.projectDir)

    }
}
