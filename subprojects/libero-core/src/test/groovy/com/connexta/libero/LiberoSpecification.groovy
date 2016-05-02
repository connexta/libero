package com.connexta.libero

import org.ajoberstar.grgit.BranchStatus
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths


class LiberoSpecification extends Specification {
    @Rule TemporaryFolder tmp
    Libero libero = new Libero()
    File localRepo
    File remoteRepo
    File remoteM2
    Grgit localGit
    Grgit remoteGit
    private final Options dryRunNoPushOptions = new Options([dryRun: true, quickBuild: true, force: true, gitPush: false])
    private final Options noPushOptions = new Options([dryRun: false, quickBuild: true, force: true, gitPush: false])
    private final Options deployPushOptions = new Options([dryRun: false, quickBuild: true, force: true, gitPush: true])
    private Config minimumConfig
    private Config configWithRepo

    def setup() {
        localRepo = tmp.newFolder('localRepo')
        remoteRepo = tmp.newFolder('remoteRepo')
        remoteM2 = tmp.newFolder('m2')
        remoteGit = Grgit.init(dir: remoteRepo.absolutePath)
        Files.copy(this.getClass().getResourceAsStream('/gitRepo/pom.xml'), Paths.get(remoteRepo.absolutePath, 'pom.xml'))
        Files.copy(this.getClass().getResourceAsStream('/gitRepo/README.md'), Paths.get(remoteRepo.absolutePath, 'README.md'))
        Files.copy(this.getClass().getResourceAsStream('/gitRepo/bin.xml'), Paths.get(remoteRepo.absolutePath, 'bin.xml'))
        remoteGit.add(patterns: [ '.' ])
        remoteGit.commit(message: 'Initial Commit')
        localGit = Grgit.clone(dir: localRepo, uri: remoteGit.repository.rootDir.toURI())
        Files.copy(this.getClass().getResourceAsStream('/gitRepo/laterFile.md'), Paths.get(remoteRepo.absolutePath, 'laterFile.md'))
        remoteGit.add(patterns: [ 'laterFile.md' ])
        remoteGit.commit(message: 'Added more files')
        remoteGit.checkout(branch: 'fooBranch', createBranch: true)
        Files.copy(this.getClass().getResourceAsStream('/gitRepo/branchFile.txt'), Paths.get(remoteRepo.absolutePath, 'branchFile.txt'))
        remoteGit.add(patterns: [ 'branchFile.txt' ])
        remoteGit.commit(message: 'Added a file to a branch')
        remoteGit.checkout(branch: 'master')
        minimumConfig = new Config(projectDir: localRepo.absolutePath)
        configWithRepo = new Config(projectDir: localRepo.absolutePath, mavenRepo: "foo::default::file://${remoteM2.absolutePath}")
    }

    def "it should checkout the default branch when none is provided"() {
        when:
            libero.run(dryRunNoPushOptions, minimumConfig)
        then:
            localGit.branch.current.name == "master"
    }

    def "it should compute the releaseVersion and the nextVersion when none are specified"() {
        setup: "creating basic config"
            def releaseVersion = "1.0.0"
            def nextVersion = "1.0.1-SNAPSHOT"
        when:
            libero.run(dryRunNoPushOptions, minimumConfig)
        then:
            minimumConfig.releaseVersion == releaseVersion
            minimumConfig.nextVersion == nextVersion
    }

    def "it should perform a pull for the source branch"() {
        when:
            libero.run(dryRunNoPushOptions, minimumConfig)
        then:
            BranchStatus localMaster = localGit.branch.status(name: minimumConfig.ref)
            localMaster.behindCount == 0
    }

    def "it should create commits/tags for the release and development version"() {
        when:
            libero.run(dryRunNoPushOptions, minimumConfig)
        then:
            BranchStatus localMaster = localGit.branch.status(name: minimumConfig.ref)
            Tag[] tags = localGit.tag.list()
            localMaster.aheadCount == 2
            tags[0].name == minimumConfig.releaseName
    }

    def "it should push commits/tags to the remote branch"() {
        when:
            libero.run(deployPushOptions, configWithRepo)
        then:
            BranchStatus localMaster = localGit.branch.status(name: configWithRepo.ref)
            localMaster.aheadCount == 0
            localMaster.behindCount == 0
    }

    def "it should not push commits/tags to the remote branch when push is disabled"() {
        when:
            libero.run(noPushOptions, configWithRepo)
        then:
            BranchStatus localMaster = localGit.branch.status(name: configWithRepo.ref)
            localMaster.aheadCount == 2
            localMaster.behindCount == 0
    }

    def "it should not deploy anything during a dry run"() {
        when:
            libero.run(dryRunNoPushOptions, configWithRepo)
        then:
            remoteM2.list().size() == 0
    }

    def "it should deploy artifacts to the specified remote repo"() {
        when:
            libero.run(deployPushOptions, configWithRepo)
        then:
            remoteM2.list().size() == 1
    }

    def "it should replace properties in the version fields"() {
        setup: "create config with properties in version"
            def config = new Config(
                    projectDir: localRepo.absolutePath,
                    releaseVersion: '${baseVersion}-${timestamp}',
                    nextVersion: '${baseVersion}-SNAPSHOT'
            )
        when:
            libero.run(dryRunNoPushOptions, config)
        then:
            config.nextVersion == "1.0.0-SNAPSHOT"
            config.releaseVersion == "${config.projectProperties.baseVersion}-${config.projectProperties.timestamp}"
    }

    def "it should create a tag containing the resolved release version when a release version conatins a property reference"() {
        setup: "creating config with release properties"
        minimumConfig.releaseVersion = '${baseVersion}-${timestamp}'
        minimumConfig.nextVersion = '${baseVersion}-SNAPSHOT'

        when:
        libero.run(dryRunNoPushOptions, minimumConfig)
        then:
        minimumConfig.releaseName == "${minimumConfig.projectName}-${minimumConfig.projectProperties.baseVersion}-${minimumConfig.projectProperties.timestamp}"
        minimumConfig.nextVersion == "${minimumConfig.projectProperties.baseVersion}-SNAPSHOT"
    }

    @Ignore
    def "it should update properties in prior to release"() {
        setup: "create config with property replacements"
            def config = new Config(
                    projectDir: localRepo.absolutePath,
                    preProps: ["baz.qux": "1.2.3"],
                    postProps: ["baz.qux": "1.2.4-SNAPSHOT"]
            )
        when:
            libero.run(dryRunNoPushOptions, config)
        then:
            localGit.checkout(branch: config.releaseName, orphan: true)
            def pom = new XmlSlurper().parse(new File(config.projectDir, "pom.xml"))
            pom.properties == "1.2.3"
    }

    @Ignore
    def "it should update properties after release"() {
        setup: "create config with property replacements"
            def config = new Config(
                    projectDir: localRepo.absolutePath,
                    preProps: ["baz.qux": "1.2.3"],
                    postProps: ["baz.qux": "1.2.4-SNAPSHOT"]
            )
        when:
            libero.run(dryRunNoPushOptions, config)
        then:
            localGit.branch.current.name
            def pom = new XmlSlurper().parse(new File(config.projectDir, "pom.xml"))
            pom.properties == "1.2.4-SNAPSHOT"
    }

    def "it should throw an exception when a build fails"() {
        setup: "create config with invalid pom"
            new File(localRepo.absolutePath, "pom.xml").delete()
            Files.copy(this.getClass().getResourceAsStream('/gitRepo/bad_pom.xml'), Paths.get(localRepo.absolutePath, 'pom.xml'))
        when:
            libero.run(dryRunNoPushOptions, minimumConfig)
        then:
            thrown IllegalStateException
    }
}
