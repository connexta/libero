package com.connexta.libera

import static com.connexta.libero.Util.printConfig

import com.connexta.libero.Config
import com.connexta.libero.Libero
import com.connexta.libero.Options
import groovy.transform.PackageScope


/**
 * Class that represents a chain of projects to be released. Each project contains a name, a release
 * version, a next version and a reference to the next project to release.
 */
class Project {
    final Libero libero
    final String projectName
    final String releaseVersion
    final String nextVersion
    final Project nextProject;
    Project previousProject = null;

    // Project class that doesn't release anything. Used as the last project in the release chain.
    @PackageScope
    static final Project NO_OP_PROJECT = new Project(null, "", null) {

        @Override
        void release(Options options) {
            // Last Project in the chain that does nothing and stops the release process
        }
    }

    /**
     * Constructor. Assumes that all input parameters have already been validated.
     *
     * @param libero reference to the {@link Libero} object to use to perform the releases
     * @param projectInfo information about the project to release in the
     *                    "<project>[:<releaseVersion>[:<nextVersion>]]" format
     * @param nextProject reference to the next project to release. {@link #NO_OP_PROJECT} must
     *                    be used if this project is the last one to be released.
     */
    protected Project(Libero libero, String projectInfo, Project nextProject) {
        def strings = projectInfo.tokenize(":")

        this.libero = libero
        this.projectName = strings[0]
        this.releaseVersion = strings[1]
        this.nextVersion = strings[2]
        this.nextProject = nextProject

        if (nextProject != null && nextProject != NO_OP_PROJECT) {
            this.nextProject.previousProject = this
        }
    }

    /**
     * Creates {@link Project} instances from a list of project arguments, chaining them and
     * returning the first one to release.
     *
     * @param libero reference to the Libero object used to perform the actual release operations
     * @param projectArguments list of strings containing the release information about the
     *                         projects in the "<project>[:<releaseVersion>[:<nextVersion>]]" format
     * @return reference to the first project to release
     */
    static Project createProjects(Libero libero, List<String> projectArguments) {
        def nextProject = NO_OP_PROJECT

        projectArguments.reverseEach {
            projectArgument ->
                nextProject = new Project(libero, projectArgument, nextProject)
        }

        return nextProject
    }

    /**
     * Releases this project. If successful, the next project will automatically be released.
     *
     * @param options release options provided at the command line
     */
    void release(Options options) {
        doRelease(options, getConfig())
    }

    protected Config loadConfig() {
        def config = new Config();
        config.loadConfig(new File("${projectName}.yml"))
        return config
    }

    private doRelease(Options options, Config config) {

        println "Releasing project ${projectName} ${releaseVersion} using the following configuration:"
        printConfig(options, config)

        libero.run(options, config)
        nextProject.release(options)
    }

    private getConfig() {
        def config = loadConfig()

        config.projectName = projectName
        config.releaseVersion = releaseVersion
        config.nextVersion = nextVersion ?: config.nextVersion
        config.projectDir = System.getProperty('user.dir') + File.separator + projectName

        config.preProps = [:]
        config.postProps = [:]
        addPreAndPostVersions(config)

        return config
    }

    protected addPreAndPostVersions(Config config) {
        if (previousProject != null && previousProject != NO_OP_PROJECT) {
            config.preProps.put("${previousProject.projectName}.version", "${previousProject.releaseVersion}")
            config.postProps.put("${previousProject.projectName}.version", "${previousProject.nextVersion}")
            previousProject.addPreAndPostVersions(config)
        }
    }
}
