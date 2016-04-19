package com.connexta.libero.cli

import com.connexta.libero.Config
import com.connexta.libero.Libero
import com.connexta.libero.Options
import com.connexta.libero.Util

import java.nio.file.Path
import java.nio.file.Paths

def run(args) {
    final Libero libero = new Libero()
    final Util util = new Util()
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

    String WORKING_DIRECTORY = System.getProperty('user.dir')
    String USER_HOME = System.getProperty('user.home')

    config.projectDir = options.arguments()[0] ?: WORKING_DIRECTORY
    String baseName = new File(config.projectDir).name

    Path projectConfig = Paths.get(config.projectDir, ".libero.yml")
    Path userConfig = Paths.get(USER_HOME, ".libero", baseName, ".libero.yml")

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
            config.loadConfig(new File((String)options.c))
        }
    }
    else
    {
        if (projectConfig.toFile().exists()) {
            config.loadConfig(projectConfig.toFile())
        }
        if (userConfig.toFile().exists()) {
            config.loadConfig(userConfig.toFile())
        }
    }

    Options liberoOptions = new Options(options)

    config.sourceRemote = options.s ?: config.sourceRemote
    config.destRemote = options.d ?: config.destRemote
    config.ref = options.r ?: config.ref
    config.destBranch = options.b ?: config.destBranch
    config.mavenRepo = options.m ?: config.mavenRepo ?: null
    config.preProps = util.stringToMap(options."pre-props") ?: config.preProps
    config.postProps = util.stringToMap(options."post-props") ?: config.postProps
    config.releaseVersion = options.v ?: config.releaseVersion
    config.nextVersion = options.n ?: config.nextVersion

    libero.run(liberoOptions, config)
}

run(args)
