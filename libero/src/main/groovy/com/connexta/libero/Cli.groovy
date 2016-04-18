package com.connexta.libero

import java.nio.file.Paths

def run(args) {
    def cli = new CliBuilder(usage: 'libero -[fhpt] [src]', header: 'I Release You!')

    cli.with {
        c longOpt: 'config', args: 1, argName: 'configFile', 'Path to yaml config file, if none provided, ' +
                'will search the project dir for .libero.yml, then in ~/.libero/[projectName].yaml. If none found, ' +
                'it will look for command line parameters for all options'
        d longOpt: 'dest-remote', args: 1, argName: 'destRepo', 'Destination repo for the release process. ' +
                'Can be either a full url, or a repo slug for github repos. If not specified the destination ' +
                'will be the same as the source repo'
        m longOpt: 'maven-repo', args: 1, argName: 'mavenRepo', 'Maven release repository to deploy the release artifacts to. use the form ID:LAYOUT:URL'
        r longOpt: 'ref', args: 1, argName: 'ref', 'Git ref to release from, can be any supported git ref'
        s longOpt: 'source-remote', args: 1, argName: 'sourceRepo', 'Git remote repo to use for the initial checkout for the release process'
        v longOpt: 'release-version', args: 1, argName: 'releaseVersion', 'Target version for the release'
        n longOpt: 'next-version', args: 1, argName: 'nextVersion', 'Next version after the release, default: auto-increment'
        b longOpt: 'dest-branch', args: 1, argName: 'destBranch', 'Branch to push to on the destination remote'
        _ longOpt: 'pre-props', args: 1, argName: 'preProps', 'comma separated list of pre-release property updates, specify in the form "propertyName=propertyValue"'
        _ longOpt: 'post-props', args: 1, argName: 'postProps', 'comma separated list of post-release property updates, specify in the form "propertyName=propertyValue"'

        h longOpt: 'help', 'Show usage information'
        p longOpt: 'push', 'Push commits and tags'
        t longOpt: 'test', 'Run in dry run mode'
        f longOpt: 'force', 'Skips confirmation of options and executes the release'
        q longOpt: 'quick-build', 'Executes a quick build instead of a full build (Not Recommended!!!)'
    }

    // Show Usage when -h or --help is specified, or if no arguments are given
    OptionAccessor options = cli.parse(args)

    Config config = new Config()
    Util util = new Util()

    String WORKING_DIRECTORY = System.getProperty('user.dir')
    String USER_HOME = System.getProperty('user.home')

    config.projectDir = options.arguments()[0] ?: WORKING_DIRECTORY
    config.projectName = new File(config.projectDir).getName()

    if (options.h || !new File(config.projectDir, "pom.xml").exists()) {
        cli.usage()
        System.exit(0)
    }

    // Get Config
    if (options.c) {
        if (!new File((String)options.c).exists()) {
            println "Config File: ${options.c} does not exist"
            System.exit(1)
        }
        else {
            configFile = new File((String)options.c)
            config.loadConfig()
        }
    }
    else
    {
        if (new File(Paths.get(config.projectDir, ".libero.yml").toUri()).exists()) {
            config.configFile = new File(config.projectDir, ".libero.yml")
            config.loadConfig()
        }
        if (new File(Paths.get(USER_HOME, ".libero", config.projectName).toUri()).exists()) {
            config.configFile = new File(Paths.get(USER_HOME, ".libero", config.projectName, ".libero.yml").toUri())
            config.loadConfig()
        }
    }

    config.sourceRemote = options.s ?: config.sourceRemote ?: "origin"
    config.destRemote = options.d ?: config.destRemote ?: config.sourceRemote
    config.ref = options.r ?: config.ref ?: "refs/remotes/${config.sourceRemote}/master"
    config.destBranch = options.b ?: config.destBranch ?: "master"
    config.mavenRepo = options.m ?: config.mavenRepo ?: null
    config.preProps = util.stringToMap(options."pre-props") ?: config.preProps ?: null as Map
    config.postProps = util.stringToMap(options."post-props") ?: config.postProps ?: null as Map
    config.startVersion = util.readStartVersion(config.projectDir)
    config.releaseVersion = options.v ?: config.releaseVersion ?: util.createReleaseVersion(config.startVersion)
    config.nextVersion = options.n ?: config.nextVersion ?: util.incrementVersion(config.releaseVersion)
    config.gitPush = options.p ?: config.gitPush ?: false
    config.quickBuild = options.q ?: config.quickBuild ?: false
    // TODO: dry run should do everything except maven deploy and git push (also remove tags and reset after run)
    config.dryRun = options.t ?: config.dryRun ?: config.dryRun ?: false
    config.force = options.f ?: config.force ?: config.force ?: false

    Libero libero = new Libero(config, util)
    libero.run()
}

run(args)
