# Libera

Multi-Project Maven Release Helper script

This script is to help automate the process of releasing multiple projects that depend on each other.

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

* Supports releasing one or more projects in sequence
* Automatically updates down-stream projects' pom files to use previous projects' newly released version
* Uses Libero to release each individual project

## Usage

```man
usage: libera [-fhpqt] project1[:release-version[:next-version]]
              [project2[:release-version[:next-version]]] ...

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
 -f,--force         Skips confirmation of options and executes the release
 -h,--help          Show usage information
 -p,--push          Push commits and tags
 -q,--quick-build   Executes a quick build instead of a full build (not
                    recommended when not in dry run mode!)
 -t,--test          Run in dry run mode
```
