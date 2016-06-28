package com.connexta.libero

import static org.ho.yaml.Yaml.load

class Config {

    String projectDir
    String projectName
    String sourceRemote
    String destRemote
    String ref
    String destBranch

    String startVersion
    String releaseVersion
    String nextVersion
    String releaseName

    String mavenRepo
    String mavenSettings
    Map preProps
    Map postProps

    String originalBranch

    Util util = new Util()

    Map projectProperties = new HashMap()

    def loadConfig(File configFile) {
        def project = load(configFile).project

        // Git settings
        sourceRemote = project.git.source.remote
        ref = project.git.source.ref
        destRemote = project.git.destination.remote
        destBranch = project.git.destination.branch

        // Maven settings
        mavenRepo = project.maven.repo
        if (project.maven.containsKey("properties")) {
            preProps = project.maven.properties."pre-release"
            postProps = project.maven.properties."post-release"
        }
        mavenSettings = project.maven.settings

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
