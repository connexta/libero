# libero-core
core library for performing releases

This is the heart of libero, where the release process is assembled and executed

## Building
This project requires gradle

### Full build
```bash
gradle build
```

## Features

* supports releasing on a forked repo, while still using the upstream changes
  * uses multiple remotes
* release from any supported git revision
* commit release to any git branch on any remote
* disable automatic git pushing
* release dry run (runs everything except the maven deploy process)
* quick build (for testing purposes only, not recommended for production uses)

## Usage

Libero needs to be provided with an `Options` object and a `Config` object prior to being run.

```groovy
Options options = new Options([
    dryRun: false, 
    quickBuild: true, 
    force: true, 
    gitPush: false ])
// At a minimum, config must contain a path to the project root directory
Config config = nwe Config(
    projectDir: localRepo.absolutePath)
```

### Options

Supported options are:

* `gitPush`: `boolean`, default: `false`
    * Enables pushing of commits and tags
* `dryRun`: `boolean`, default: `false`
    * Enables dry-run mode (skips deploy phase)
* `force`: `boolean`, default: `false`
    * Bypasses user confirmation
* `quickBuild`: `boolean`, default: `false`
    * Skips tests during build phase (for testing purposes)

### Config

Config Parameters are:

* `projectDir`: `String`, root project directory (only required parameter)
* `projectName`: `String`, name of project
* `sourceRemote`: `String`, name of remote to pull from
* `destRemote`: `String`, name of remote to push to
* `ref`: `String`, git reference/branch to build release from
* `startVersion`: `String`, Auto-computed version from pom file
* `releaseVersion`: `String`, version to release
* `nextVersion`: `String`, next development version
* `releaseName`: `String`, name of the release. used for git tag
* `mavenRepo`: `String`, alternative maven repo for deployment
    * follows the syntax `<server-id>::<layout>::<url>`
* `preProps`: `Map`, map of key, value pairs to replace in pom prior to release
* `postProps`: `Map`, map of key, value pairs to replace in pom after release
