package com.connexta.libero

import spock.lang.Specification

class ConfigSpecification extends Specification {

    def "Configurations can be loaded from a file"() {
        setup: "creating initial config opject and file reference"
            File file = new File(this.getClass().getResource('/test-config.yml').toURI())
            Config config = new Config()
        when:
            config.loadConfig(file)
        then:
            config.destBranch == "master"
            config.destRemote == "origin"
            config.sourceRemote == "origin"
    }

    def "Project Properties in config values can be replaced"() {
        setup: "Creating initial config with property references"
            Config config = new Config(
                    releaseVersion: '${baseVersion}.${timestamp}',
                    projectProperties: [baseVersion: "1.0.0", timestamp: "foo"]
            )
            def expected = "1.0.0.foo"
        when:
            config.resolveProperties()
        then:
            config.releaseVersion == expected
    }
}
