package com.connexta.libera

import com.connexta.libero.Config
import com.connexta.libero.Libero
import com.connexta.libero.Options
import spock.lang.Specification

class TestProject extends Specification {
    def libero = Mock(Libero)

    def project1 = [name: "project1", releaseVersion: "1.0.0", nextVersion: "1.1.0"]
    def project2 = [name: "project2", releaseVersion: "2.0.0", nextVersion: "2.1.0"]
    def project3 = [name: "project3", releaseVersion: "3.0.0", nextVersion: "3.1.0"]

    class ProjectUnderTest extends Project {
        Config config = new Config()

        ProjectUnderTest(Libero libero, String projectInfo, Project nextProject) {
            super(libero, projectInfo, nextProject)
        }

        @Override
        protected Config loadConfig() {
            config.loadConfig(new File(getClass().getResource("/project.yml").toURI()))
            return config
        }
    }

    def "Create list of projects when no declaration provided"() {
        given:
        def emptyProjectDeclarationList = []

        when:
        def projects = Project.createProjects(libero, emptyProjectDeclarationList)

        then:
        projects == Project.NO_OP_PROJECT
    }

    def "Create list of projects from multiple declarations"() {
        given:
        def projectDeclarations = [
                "${project1.name}:${project1.releaseVersion}:${project1.nextVersion}",
                "${project2.name}:${project2.releaseVersion}:${project2.nextVersion}"
        ]

        when:
        def projects = Project.createProjects(libero, projectDeclarations)

        then:
        def firstProject = projects
        firstProject.projectName == project1.name
        firstProject.releaseVersion == project1.releaseVersion
        firstProject.nextVersion == project1.nextVersion

        def secondProject = firstProject.nextProject
        secondProject.projectName == project2.name
        secondProject.releaseVersion == project2.releaseVersion
        secondProject.nextVersion == project2.nextVersion
        secondProject.previousProject == firstProject
        secondProject.nextProject == Project.NO_OP_PROJECT
    }

    def "Create Project with only a project name"() {
        given:

        when:
        def project = new Project(libero, project1.name, Project.NO_OP_PROJECT)

        then:
        project.projectName == project1.name
        !project.releaseVersion
        !project.nextVersion
    }

    def "Create Project with a project name and release version"() {
        given:

        when:
        def project = new Project(libero, "${project1.name}:${project1.releaseVersion}", Project.NO_OP_PROJECT)

        then:
        project.projectName == project1.name
        project.releaseVersion == project1.releaseVersion
        !project.nextVersion
    }

    def "Create Project with a project name, release version and next version"() {
        given:

        when:
        def project =
                new Project(libero,
                        "${project1.name}:${project1.releaseVersion}:${project1.nextVersion}",
                        Project.NO_OP_PROJECT)

        then:
        project.projectName == project1.name
        project.releaseVersion == project1.releaseVersion
        project.nextVersion == project1.nextVersion
    }

    def "Libero and next project are set in Project"() {
        given:
        def nextProject = Mock(Project)

        when:
        def project = new Project(libero, project1.name, nextProject)

        then:
        project.libero == libero
        project.nextProject == nextProject
    }

    def "Release single project"() {
        given:
        def project =
                new ProjectUnderTest(libero,
                        "${project1.name}:${project1.releaseVersion}:${project1.nextVersion}",
                        Project.NO_OP_PROJECT)

        when:
        project.release(Mock(Options))

        then:
        // Not sure why but libero.run(config) didn't work here...
        // libero.run(project.config)
        libero.run(project.config) >> { Config c -> c == project.config }
        project.config.projectDir == System.getProperty('user.dir') + File.separator + project1.name
        project.config.releaseVersion == project1.releaseVersion
        project.config.nextVersion == project1.nextVersion
        !project.config.preProps
        !project.config.postProps
    }

    def "Projects are properly chained"() {
        given:
        def firstProject = Project.createProjects(libero, [
                "${project1.name}:${project1.releaseVersion}:${project1.nextVersion}",
                "${project2.name}:${project2.releaseVersion}:${project2.nextVersion}",
                "${project3.name}:${project3.releaseVersion}:${project3.nextVersion}"
        ])

        def secondProject = firstProject.nextProject
        def thirdProject = secondProject.nextProject

        expect:
        thirdProject.nextProject == Project.NO_OP_PROJECT
        thirdProject.previousProject == secondProject
        secondProject.previousProject == firstProject
        firstProject.previousProject == null
    }

    def "Release multiple projects"() {
        given:
        def thirdProject =
                new ProjectUnderTest(libero,
                        "${project3.name}:${project3.releaseVersion}:${project3.nextVersion}",
                        Project.NO_OP_PROJECT)

        def secondProject =
                new ProjectUnderTest(libero,
                        "${project2.name}:${project2.releaseVersion}:${project2.nextVersion}",
                        thirdProject)

        def firstProject =
                new ProjectUnderTest(libero,
                        "${project1.name}:${project1.releaseVersion}:${project1.nextVersion}",
                        secondProject)

        when:
        firstProject.release(Mock(Options))

        then:
        libero.run(firstProject.config) >> { Config c -> c == firstProject.config }
        libero.run(secondProject.config) >> { Config c -> c == secondProject.config }
        libero.run(thirdProject.config) >> { Config c -> c == thirdProject.config }

        secondProject.config.projectDir == System.getProperty('user.dir') + File.separator + project2.name
        secondProject.config.releaseVersion == project2.releaseVersion
        secondProject.config.nextVersion == project2.nextVersion
        secondProject.config.preProps == ["${project1.name}.version": "${project1.releaseVersion}"]
        secondProject.config.postProps == ["${project1.name}.version": "${project1.nextVersion}"]

        thirdProject.config.projectDir == System.getProperty('user.dir') + File.separator + project3.name
        thirdProject.config.releaseVersion == project3.releaseVersion
        thirdProject.config.nextVersion == project3.nextVersion
        thirdProject.config.preProps == [
                "${project1.name}.version": "${project1.releaseVersion}",
                "${project2.name}.version": "${project2.releaseVersion}"
        ]
        thirdProject.config.postProps == [
                "${project1.name}.version": "${project1.nextVersion}",
                "${project2.name}.version": "${project2.nextVersion}"
        ]
    }
}
