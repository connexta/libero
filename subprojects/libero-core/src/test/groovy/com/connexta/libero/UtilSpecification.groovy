package com.connexta.libero

import spock.lang.*

class UtilSpecification extends Specification {

    def util = new Util()

    def "A single existing property reference should be replaced by it's value"() {
        setup: "a new property and test string is created"
            def properties = [test: "foo"]
            def data = '${test}bar'
            def expected = "foobar"
        when:
            data = util.replaceProperties(data, properties)
        then:
            data == expected
    }

    def "Multiple existing property references should be replaced by their values"() {
        setup: "a new set of properties and a test string are created"
            def properties = [test1: "foo", test2: "bar"]
            def data = '${test1}${test2}'
            def expected = "foobar"
        when:
            data = util.replaceProperties(data, properties)
        then:
            data == expected
    }

    def "Multiple existing property references anywhere in the string should be replaced by their values"() {
        setup: "a new set of properties and a test string are created"
            def properties = [test1: "foo", test2: "bar"]
            def data = 'this ${test1} is not a ${test2}'
            def expected = "this foo is not a bar"
        when:
            data = util.replaceProperties(data, properties)
        then:
            data == expected
    }

    def "Nonexistent property references should not be replaced"() {
        setup: "test string is created with a non-existent property reference"
            def properties = new HashMap()
            def data = 'this prop ${foo} does not exist'
            def expected = data
        when:
            data = util.replaceProperties(data, properties)
        then:
            data == expected
    }

    def "Existing properties should be replaced while non-existent properties are not replaced"() {
        setup: "test string and properties are created"
            def properties = [test: "foo"]
            def data = '${test} is a property, ${bar} is not'
            def expected = 'foo is a property, ${bar} is not'
        when:
            data = util.replaceProperties(data, properties)
        then:
            data == expected
    }

    def "Maps should be parsed from strings"() {
        setup: "create string map"
            def data = "foo=bar,this=that"
            def expected = [foo: "bar", this: "that"]
        when:
            def map = util.stringToMap(data)
        then:
            map == expected
    }

    def "false or null options should be returned back from the stringToMap method when no map exists"() {
        setup: "create falsey string"
            def data = false
            def expected = null
        when:
            def map = util.stringToMap(data)
        then:
            map == expected
    }

    def "false strings should be returned back as null from the stringToMap method"() {
        setup: "create falsey string"
            def data = "false"
            def expected = null
        when:
            def map = util.stringToMap(data)
        then:
            map == expected
    }

    def "a version can be extracted from a pom file"() {
        setup: "provide test pom"
            File pom = new File(this.getClass().getResource("/gitRepo/pom.xml").toURI())
            def expected = "1.0.0-SNAPSHOT"
        when:
            def version = util.getPomVersion(pom)
        then:
            version == expected
    }

    def "a release version can be created from a development version"() {
        setup: "provide a development version"
            def version = "1.0.0-SNAPSHOT"
            def expected = "1.0.0"
        when:
            def releaseVersion = util.getBaseVersion(version)
        then:
            releaseVersion == expected
    }

    def "a release version can be auto-incremented to the next development version"() {
        setup: "provide a release version"
            def version = "1.0.0"
            def expected = "1.0.1-SNAPSHOT"
        when:
            def nextVersion = util.incrementVersion(version)
        then:
            nextVersion == expected
    }

    @Ignore
    def "a release version containing more than the standard major.minor.patch can be auto-incremented"() {
        setup: "provide a release version of format major.minor.patch.buildNumber"
            def version = "1.0.0.11"
            def expected = "1.0.0.12-SNAPSHOT"
        when:
            def nextVersion = util.incrementVersion(version)
        then:
            nextVersion == expected
    }

    @Ignore
    def "a release version containing non-numeric additions past the patch version can be auto-incremented"() {
        setup: "provide a release version with a non-numeric ending"
            def version = "1.0.0.RC1"
            def expected = "1.0.1-SNAPSHOT"
        when:
            def nextVersion = util.incrementVersion(version)
        then:
            nextVersion == expected
    }

    @Ignore
    def "a release version containing non-numeric additions past the minor version with a '-' can be auto-incremented"() {
        setup: "provide a release version with a non-numeric ending"
            def version = "1.0.0-RC1"
            def expected = "1.0.1-SNAPSHOT"
        when:
            def nextVersion = util.incrementVersion(version)
        then:
            nextVersion == expected
    }
}
