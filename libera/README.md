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
