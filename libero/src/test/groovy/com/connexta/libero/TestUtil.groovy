package com.connexta.libero

import spock.lang.*

class TestUtil extends Specification {

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
}
