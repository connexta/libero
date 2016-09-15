# Libero
_I Release You_

Maven Release Helper script

This script is to help automate the process of performing a maven release against a git repo without resorting to the maven-release-plugin.

## Building
This project requires gradle

### Full build
```bash
gradle build
```

distribution files will be under `build/distributions`

### Test build
to run the build and auto-expand the distribution script run:
```bash
gradle installDist
```

installed distribution will be under `build/install`

## Features

* supports releasing on a forked repo, while still using the upstream changes
  * uses multiple remotes
* release from any supported git revision
* commit release to any git branch on any remote
* disable automatic git pushing
* release dry run (runs everything except the maven deploy process)
* quick build (for testing purposes only, not recommended for production uses)
* supports command line parameters for all options
* supports a config file for all options
  * config files can be specified as a parameter
  * searches in `<projectdir>/.libero.yml`
  * searches in `~/.libero/<project-name>/.libero.yml`
  
## Cleaning local artifacts

Libero now supports cleaning artifacts from the local repo prior to building the project

Artifacts can be cleaned by providing the `project.maven.repo.clean` property with a list of comma separated group ids
Any artifact located at or below that group id depth will be deleted

## Using an alternate local maven repository

An alternate local maven repo can be configured by providing the `project.maven.repo.local` property with a path to use for a local repository

## Alternate deployment repository

Libero will deploy to the repository specified in the pom by default.
To configure an alternate repository for deployment use the `project.maven.repo.remote` property.
This property expects a value like `repositoryId::format::url`. If authentication is required, repository id should match a server from the maven settings file

## Custom profiles

Libero can be configured to activate a set of profiles during the execution of the release phase.

Provide a comma separated list in `project.maven.profiles`

## Custom settings file

Libero can be configured to use a custom maven settings file for any maven operations.

Provide a location of a maven settings file to `project.maven.settings`

## Pom Property replacement

Libero can replace property values in the root level project pom before and after release.

To accomplish this use the `project.maven.properties` property.

This has two sub-properties `pre-release` and `post-release` that take in `name: value` pairs.

```yaml
project:
  ...
  maven:
    properties:
      pre-release:
        foo.version: 1.0.0
        bar.version: 1.2.0
      post-release:
        foo.version: 1.0.1-SNAPSHOT
        bar.version: 1.2.1-SNAPSHOT
  ...
```

## Custom git commit prefix

Libero defaults to creating git commits with the prefix `"[libero]"`, 
this can be changed using the `project.git.message.prefix`

## Project properties

There are several properties that are generated when the project is analyzed at begginning of the run.
These properties can be used in config files and in cli paramaters. 

Usage: `${name}`

* `timestamp`: Date and time the run was started, format is `YYYY-MM-DDThhmmssZ`
* `baseVersion`: Starting version of the project, stripped of `SNAPSHOT`

## Cli

```man
usage: libero -[fhpt] [src]
I Release You!
 -b,--dest-branch <destBranch>           Branch to push to on the
                                         destination remote
 -c,--config <configFile>                Path to yaml config file, if none
                                         provided, will search the project
                                         dir for .libero.yml, then in
                                         ~/.libero/[projectName].yaml. If
                                         none found, it will look for
                                         command line parameters for all
                                         options
 -d,--dest-remote <destRepo>             Destination repo for the release
                                         process. Can be either a full
                                         url, or a repo slug for github
                                         repos. If not specified the
                                         destination will be the same as
                                         the source repo
 -f,--force                              Skips confirmation of options and
                                         executes the release
 -h,--help                               Show usage information
 -m,--maven-repo <mavenRepo>             Maven release repository to
                                         deploy the release artifacts to.
                                         use the form ID:LAYOUT:URL
 -n,--next-version <nextVersion>         Next version after the release,
                                         default: auto-increment
 -p,--push                               Push commits and tags
    --post-props <postProps>             comma separated list of
                                         post-release property updates,
                                         specify in the form
                                         "propertyName=propertyValue"
    --pre-props <preProps>               comma separated list of
                                         pre-release property updates,
                                         specify in the form
                                         "propertyName=propertyValue"
 -q,--quick-build                        Executes a quick build instead of
                                         a full build (Not Recommended!!!)
 -r,--ref <ref>                          Git ref to release from, can be
                                         any supported git ref
 -s,--source-remote <sourceRepo>         Git remote repo to use for the
                                         initial checkout for the release
                                         process
 -t,--test                               Run in dry run mode
 -v,--release-version <releaseVersion>   Target version for the release
```

## Config File Reference

```yaml
 project:
   git:
     source:
       remote: "origin"
       ref: "master"
     destination:
       remote: "origin"
       branch: "master"
     message:
       prefix: "[release]"
   maven:
     repo:
       local: "/path/to/local/repo"
       remote: "releases::default::http://nexus.fake.site/nexus/content/repositories/releases/"
       clean: "foo.bar,baz.qux"
     profiles: "release"
     properties:
       pre-release:
         foo.property: "barValue"
         bar.property: "fooValue"
       post-release:
         this: "that"
   versions:
     release: "1.2.3-${timestamp}"
     development: "1.2.4-SNAPSHOT"
```