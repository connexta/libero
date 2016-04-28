package com.connexta.libero

import java.util.regex.Matcher

/**
 * find property references in a string and replace them with the corresponding value from a set of properties
 * @param data String data to check for property references
 * @param properties
 * @return string with properties replaced
 */
String replaceProperties(String data, Map properties) {
    Matcher findProp = (data =~ '\\$\\{([^}]*)\\}')
    for (prop in findProp) {
        String key = prop[1]
        String value = properties."$key"
        if (value != null) {
            String filter = /\$\{/ + key + /\}/
            data = data.replaceAll(filter, "${value}")
        }
    }
    data
}


/**
 * compute the difference between the current time and a start time
 * @param startTime
 * @return duration
 */
String computeDuration(startTime) {

}

/**
 * Executes a shell command, if the command fails it will print out an error message and
 * exit with the same exit code as the command
 * @param command command to execute
 * @param workingDir working directory for the command
 */
void executeCommand(String command, String workingDir) {
    File directory = new File(workingDir)
    println "Running Command: ${command}"
    def process = new ProcessBuilder(addShellPrefix(command))
            .directory(directory)
            .redirectErrorStream(true)
            .start()
    process.inputStream.eachLine {println it}
    process.waitFor()

    int exitCode = process.exitValue()
    if (exitCode > 0) {
        println "Command: ${command} failed to execute properly"
        System.exit(exitCode)
    }
}

/**
 * inserts a shell prefix before a command
 */
private String[] addShellPrefix(String command) {
    String[] commandArray = new String[3]
    commandArray[0] = "sh"
    commandArray[1] = "-c"
    commandArray[2] = command
    return commandArray
}

/**
 * Converts a comma separated string of key=value pairs to a map
 * @param data
 * @return map
 */
def stringToMap(data) {
    if (data && data != "false") {
        data.split(',').inject([:]) { LinkedHashMap map, token ->
            token.split('=').with { map[it[0]] = it[1]
            }
            map
        }
    }
    else {
        null
    }
}

/**
 * read the initial version from the pom
 * @param projectDir
 * @return
 */
String getPomVersion(File pomFile) {
    def pom = new XmlSlurper().parse(pomFile)
    pom.version.toString()
}

/**
 * create default release version string. just removes snapshot from the initial version.
 * @param startVersion
 * @return
 */
String getBaseVersion(startVersion) {
    startVersion.replace("-SNAPSHOT", "")
}

/**
 * auto-increment version
 * @param releaseVersion
 * @return
 */
String incrementVersion(releaseVersion) {
    // TODO: check for expected pattern
    version = releaseVersion.replaceAll("[^\\d.]", "").split('\\.')
    version[2] = ((version[2] as Integer) + 1) as String
    incrementedVersion = version.join('.')
    String newVersion = "${incrementedVersion}-SNAPSHOT"
}

/**
 * prints the configuration and options for libero
 * @param options
 * @param config
 * @return
 */
def static printConfig(Options options, Config config) {
    println "============== RELEASE PARAMETERS =============="
    println "____________ GENERAL ____________"
    println "Project Directory: ${config.projectDir}"
    println "Project Name: ${config.projectName}"
    println "Dry Run: ${options.dryRun}"
    println "Force: ${options.force}"
    println "______________ GIT ______________"
    println "Source git Remote: ${config.sourceRemote}"
    println "Source git Ref: ${config.ref}"
    println "Destination git Remote: ${config.destRemote}"
    println "Destination git Branch: ${config.destBranch}"
    println "Push Git tags/commits: ${options.gitPush}"
    println "_____________ MAVEN _____________"
    println "Source Version: ${config.startVersion}"
    println "Release Version: ${config.releaseVersion}"
    println "Next Development Version: ${config.nextVersion}"
    println "Pre-Release property updates: ${config.preProps}"
    println "Post-Release property updates: ${config.postProps}"
    println "Maven Release Repo: ${config.mavenRepo}"
    println "Quick Build: ${options.quickBuild}"
    println "============ END RELEASE PARAMETERS ============"
}
