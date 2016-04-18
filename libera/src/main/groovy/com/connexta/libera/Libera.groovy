package com.connexta.libera

import com.connexta.libero.Libero
import org.apache.commons.cli.MissingArgumentException

/**
 * Class used to validate and extract the command line options and project arguments.
 */
class Arguments {
    private static final USAGE_HEADER = '''
        Releases a series of inter-dependent projects.

        conventions:
          - A <project>.yml file, that contains all the default release
            configuration for that project, must exit in the current working
            directory ($CWD).
          - All projects to release must be cloned under the same parent
            directory ($CWD/project1, $CWD/project2, etc.).
          - The script must be run from the common parent directory of the
            projects' local git repositories ($CWD).
          - The root pom.xml of each project (excluding the first one) must
            contain a property named <previous project>.version that will be
            updated to use the previous projects' version.
          - The <release-version> argument indicates the version to use when
            releasing that project and the version that will be used in the
            pom file of the following project when released.

        example:
          libera foo:2.9.0-RC4:2.9.1-SNAPSHOT bar:4.3.1-RC3

          Assuming that the current working directory is $CWD, the previous
          command will release version 2.9.0-RC4 of the foo project using the
          configuration found in $CWD/foo.yml and the local git repository
          located in $CWD/foo.
          It will then update the $CWD/bar/pom.xml file's <foo.version/>
          property to 2.9.0-RC4 and release version 4.3.1-RC3 of the bar
          project located in $CWD/bar using the $CWD/bar.yml configuration
          file.
          Finally, the script will update the version of foo back to
          2.9.1-SNAPSHOT (both in the foo repository and the $CWD/bar/pom.xml
          file), and update the bar version to 4.3.1-SNAPSHOT

        options:
        '''.stripIndent()

    private CliBuilder cli
    private OptionAccessor options

    /**
     * Constructor that accepts the command line arguments and parses them.
     *
     * @param args command line arguments
     */
    Arguments(args) {
        cli = new CliBuilder(usage: 'libera [-fhpqt] project1[:release-version[:next-version]] ' +
                '[project2[:release-version[:next-version]]] ...',
                header: USAGE_HEADER, stopAtNonOption: false)

        cli.with {
            f longOpt: 'force', 'Skips confirmation of options and executes the release'
            h longOpt: 'help', 'Show usage information'
            p longOpt: 'push', 'Push commits and tags'
            q longOpt: 'quick-build', 'Executes a quick build instead of a full build (not recommended when not in dry run mode!)'
            t longOpt: 'test', 'Run in dry run mode'
        }

        def options = cli.parse(args)

        if (!options) {
            throw new IllegalArgumentException("Unrecognized option")
        }

        if (options.h) {
            cli.usage()
            throw new MissingArgumentException("Help requested")
        }

        this.options = options
    }

    /**
     * Gets the execution options provided at the command line
     *
     * @return execution options
     */
    Options getOptions() {
        return new Options(options)
    }

    /**
     * Gets the list of project arguments provided at the command line in the
     * "<project>[:<releaseVersion>[:<nextVersion>]]" format
     *
     * @return project arguments, one for each project to release
     */
    List<String> getProjectArguments() {
        def arguments = options.arguments()

        if (arguments.isEmpty()) {
            println 'Missing arguments'
            cli.usage()
            throw new MissingArgumentException("No projects specified")
        }

        return arguments
    }
}

/**
 * Main method.
 *
 * @param args command line arguments
 */
def int run(String[] args) {
    try {
        def libero = new Libero()
        def arguments = new Arguments(args)
        def firstProject = Project.createProjects(libero, arguments.projectArguments)
        firstProject.release(arguments.options)
        return 0
    } catch (IllegalArgumentException | MissingArgumentException e) {
        return 1
    } catch (Exception e) {
        println "Release failed: ${e.message}"
        return 2
    }
}

// Runs the script
System.exit(run(args))
