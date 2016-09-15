package com.connexta.libero

import static org.ho.yaml.Yaml.load

class Config {

    String projectDir
    String projectName
    String sourceRemote
    String destRemote
    String ref
    String destBranch
    String commitPrefix

    String startVersion
    String releaseVersion
    String nextVersion
    String releaseName

    String localRepo
    String mavenRepo
    String mavenSettings
    String cleanArtifacts
    List<String> profiles
    Map preProps
    Map postProps

    String originalBranch

    Util util = new Util()

    Map projectProperties = new HashMap()

    def loadConfig(File configFile) {
        def project = load(configFile).project

        // Git settings
        def gitSettings = project.git
        sourceRemote = gitSettings.source.remote
        ref = gitSettings.source.ref
        destRemote = gitSettings.destination.remote
        destBranch = gitSettings.destination.branch
        if (gitSettings.containsKey("message")) {
            commitPrefix = gitSettings.message.prefix
        }

        // Maven settings
        if (project.maven.repo.containsKey("local")) {
            localRepo = project.maven.repo.local
        }
        mavenRepo = project.maven.repo.remote
        if (project.maven.containsKey("properties")) {
            preProps = project.maven.properties."pre-release"
            postProps = project.maven.properties."post-release"
        }
        mavenSettings = project.maven.settings
        cleanArtifacts = project.maven.repo.clean
        profiles = project.maven.profiles

        // versions
        releaseVersion = project.versions.release
        nextVersion = project.versions.development
    }

    def resolveProperties() {
        this.sourceRemote = util.replaceProperties(sourceRemote, projectProperties)
        this.ref = util.replaceProperties(ref, projectProperties)
        this.destRemote = util.replaceProperties(destRemote, projectProperties)
        this.destBranch = util.replaceProperties(destBranch, projectProperties)
        this.releaseVersion = util.replaceProperties(releaseVersion, projectProperties)
        this.nextVersion = util.replaceProperties(nextVersion, projectProperties)
        this.releaseName = util.replaceProperties(releaseName, projectProperties)
    }
}
