package com.connexta.libera

import com.connexta.libero.Options
import spock.lang.Specification

class TestOptions extends Specification {

    def "Options properly initialized when all command line flags have been set"() {
        setup:
        def optionAccessor = Mock(OptionAccessor)

        when:
        optionAccessor.getProperty('f') >> true
        optionAccessor.getProperty('p') >> true
        optionAccessor.getProperty('q') >> true
        optionAccessor.getProperty('t') >> true

        then:
        def options = new Options(optionAccessor)
        options.force
        options.gitPush
        options.quickBuild
        options.dryRun
    }

    def "Options properly initialized when no command line flags have been set"() {
        setup:
        def optionAccessor = Mock(OptionAccessor)

        when:
        def options = new Options(optionAccessor)

        then:
        !options.force
        !options.gitPush
        !options.quickBuild
        !options.dryRun
    }
}
