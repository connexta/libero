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
 * parse the version string from the pom file located in a given git revision
 * @param projectDir working directory
 * @param rev git revision
 * @return version
 */
String getVersionFromGitRev(projectDir, rev) {

}

/**
 * Utility method to check if a revision exists
 * @param projectDir working directory
 * @param rev revision to check
 * @return false if rev does not exist
 */
boolean verifyRev(String projectDir, rev) {
    String revCheckCommand = "git rev-parse --verify -q ${rev}"
    executeCommand(revCheckCommand, projectDir)
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
 * @param command
 * @return
 */
String[] addShellPrefix(String command) {
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
String readStartVersion(String projectDir) {
    def pom = new XmlSlurper().parse(new File(projectDir, "pom.xml"))
    pom.version.toString()
}

/**
 * create default release version string. just removes snapshot from the initial version.
 * @param startVersion
 * @return
 */
String createReleaseVersion(startVersion) {
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
