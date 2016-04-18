package com.connexta.libero

import spock.lang.Specification

class TestConfig extends Specification {

    def "Configurations can be loaded from a file"() {
        setup: "creating initial config opject and file reference"
            File file = new File(this.getClass().getResource('/test-config.yml').toURI())
            Config config = new Config()
            config.configFile = file
        when:
            config.loadConfig()
        then:
            config.configFile == file
            config.destBranch == "master"
            config.destRemote == "origin"
            config.sourceRemote == "origin"
    }
}
