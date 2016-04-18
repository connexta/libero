package com.connexta.libero

import static org.ho.yaml.Yaml.load

class Config {
    Map projectProperties = new HashMap()
    String projectDir
    String projectName
    File configFile
    String sourceRemote
    String destRemote
    String ref
    String destBranch
    String startVersion
    String releaseVersion
    String nextVersion
    String mavenRepo
    Map preProps
    Map postProps
    String releaseName
    boolean quickBuild
    boolean gitPush
    boolean dryRun
    boolean force
    Util util = new Util()

    public loadConfig = { ->
        def project = load(configFile).project

        // Git settings
        sourceRemote = project.git.source.remote
        ref = project.git.source.ref
        destRemote = project.git.destination.remote
        destBranch = project.git.destination.branch
        gitPush = project.git.destination.push

        // Maven settings
        mavenRepo = project.maven.repo
        quickBuild = project.maven.quickbuild
        if (project.maven.containsKey("properties")) {
          preProps = project.maven.properties."pre-release"
          postProps = project.maven.properties."post-release"
        }

        // General settings
        dryRun = project.settings.dryRun
        force = project.settings.force

        // versions
        releaseVersion = project.versions.release
        nextVersion = project.versions.development

        resolveProperties()
    }

    public printConfig = { ->
        println "============== RELEASE PARAMETERS =============="
        println "____________ GENERAL ____________"
        println "Project Directory: ${projectDir}"
        println "Project Name: ${projectName}"
        println "Config File: ${configFile}"
        println "Dry Run: ${dryRun}"
        println "______________ GIT ______________"
        println "Source git Remote: ${sourceRemote}"
        println "Source git Ref: ${ref}"
        println "Destination git Remote: ${destRemote}"
        println "Destination git Branch: ${destBranch}"
        println "Push Git tags/commits: ${gitPush}"
        println "_____________ MAVEN _____________"
        println "Source Version: ${startVersion}"
        println "Release Version: ${releaseVersion}"
        println "Next Development Version: ${nextVersion}"
        println "Pre-Release property updates: ${preProps}"
        println "Post-Release property updates: ${postProps}"
        println "Maven Release Repo: ${mavenRepo}"
        println "Quick Build: ${quickBuild}"
        println "============ END RELEASE PARAMETERS ============"
    }

    public resolveProperties = { ->
        util.replaceProperties(sourceRemote, projectProperties)
        util.replaceProperties(ref, projectProperties)
        util.replaceProperties(destRemote, projectProperties)
        util.replaceProperties(destBranch, projectProperties)
        util.replaceProperties(releaseVersion, projectProperties)
        util.replaceProperties(nextVersion, projectProperties)
    }
}
