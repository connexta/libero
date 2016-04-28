package com.connexta.libera

import com.connexta.libero.Libero
import com.connexta.libero.Options
import spock.lang.Specification

class TestLibera extends Specification {
    def libera = new Libera()

    def "Run creates projects and starts release"() {
        given:
        GroovyMock(Project, global: true)
        def arguments = GroovyMock(Arguments, global: true)
        def projects = Mock(Project)
        def options = Mock(Options)

        def args = ["project1:1.0.0:2.0.0", "project2:1.1.0:1.2.0"] as String[]
        def projectInfo = ["project1:1.0.0:2.0.0", "project2:1.1.0:1.2.0"]

        when:
        new Arguments(args) >> arguments
        arguments.projectArguments >> projectInfo
        arguments.options >> options
        Project.createProjects(_ as Libero, _ as List<String>) >> projects
        def exitCode = libera.run(args)

        then:
        1 * projects.release(options)
        exitCode == 0
    }

    def "Run returns error exit code when release fails"() {
        given:
        GroovyMock(Project, global: true)
        def projects = Mock(Project)

        when:
        Project.createProjects(_ as Libero, _ as List<String>) >> projects
        projects.release(_) >> { throw new Exception() }
        def exitCode = libera.run(["project:1.0.0:2.0.0"] as String[])

        then:
        exitCode == 2
    }

    def "Run with invalid arguments returns error exit code"() {
        given:
        GroovyMock(Project, global: true)

        when:
        def exitCode = libera.run(["-h"] as String[])

        then:
        exitCode == 1
        0 * Project.createProjects(_, _)
    }

    def "Run with no projects returns error exit code"() {
        given:
        GroovyMock(Project, global: true)

        when:
        def exitCode = libera.run(["-fpqt"] as String[])

        then:
        exitCode == 1
        0 * Project.createProjects(_, _)
    }

    def "Run works with all supported options"() {
        given:
        GroovyMock(Project, global: true)
        def projects = Mock(Project)
        Project.createProjects(_ as Libero, _ as List<String>) >> projects

        expect:
        libera.run(args) == 0

        where:
        args                                      | _
        ["-f", "project:1.0.0:2.0.0"] as String[] | _
        ["-p", "project:1.0.0:2.0.0"] as String[] | _
        ["-q", "project:1.0.0:2.0.0"] as String[] | _
        ["-t", "project:1.0.0:2.0.0"] as String[] | _
    }

    def "Run with unsupported options returns error exit code"() {
        given:
        GroovyMock(Project, global: true)

        when:
        def exitCode = libera.run(["--invalid"] as String[])

        then:
        exitCode == 1
        0 * Project.createProjects(_, _)
    }
}
