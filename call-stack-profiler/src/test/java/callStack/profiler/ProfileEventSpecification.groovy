/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package callStack.profiler

import callStack.profiler.ProfileEvent
import spock.lang.Specification

class ProfileEventSpecification extends Specification{
//    def "Must be able to Kryo serDer"(){
//
//        ProfileEvent child1 = new ProfileEvent(name: "test", runtimeInMillis: 10)
//        ProfileEvent child2 = new ProfileEvent(name: "test", runtimeInMillis: 10)
//        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
//        event.addChild(child1)
//        event.addChild(child2)
//
//        ProfileEvent res
//        when:
//        byte [] ser = KryoSerializer.instance.serialize(event)
//        res = KryoSerializer.instance.deserialize(ser)
//
//        then:
//        res
//        res.name == event.name
//    }

    def "toString call with hierarchy should not throw exceptions :) "(){
        ProfileEvent child1 = new ProfileEvent(name: "test", runtimeInMillis: 10)
        ProfileEvent child2 = new ProfileEvent(name: "test", runtimeInMillis: 10)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)


        when:
        event.toString()
        then:
        event
    }

    def "Pretty print long running events - 1,000 ms"() {
        expect:
        profileEvent.prettyPrint() ==  result

        where:
        profileEvent			| result
        new ProfileEvent(name: "p", runtimeInMillis: 1000, numOfInvocations: 1)	                | "|-> p (1) : 1s "
        new ProfileEvent(name: "p", runtimeInMillis: 10*1000, numOfInvocations: 1)	            | "|-> p (1) : 10s "
        new ProfileEvent(name: "p", runtimeInMillis: 29999, numOfInvocations: 1)	            | "|-> p (1) : 29s 999ms"
        new ProfileEvent(name: "p", runtimeInMillis: 30*1000, numOfInvocations: 1)	            | "|-> p (1) : 30s "
        new ProfileEvent(name: "p", runtimeInMillis: 60*1000 + 1000, numOfInvocations: 1)	    | "|-> p (1) : 1m 1s "
        new ProfileEvent(name: "p", runtimeInMillis: 59*60*1000 + 59*1000, numOfInvocations: 1)	| "|-> p (1) : 59m 59s "
        new ProfileEvent(name: "p", runtimeInMillis: 62*60*1000 + 59*1000, numOfInvocations: 1)	| "|-> p (1) : 1h 2m 59s "
        new ProfileEvent(name: "p", runtimeInMillis: 143*60*1000 + 199, numOfInvocations: 1)    | "|-> p (1) : 2h 23m 199ms"
    }

    def "serialize should not throw exceptions :) "(){
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 10)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 10)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)

        ProfileEvent serialized

        when:
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(baos)
        oos.writeObject(event)
        oos.close()

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())
        ObjectInputStream ois = new ObjectInputStream(bais)
        serialized = ois.readObject()
        ois.close()

        then:
        serialized
    }

    def "demonstrate how much time is unaccounted for within its children"() {
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 4)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 5)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)

        when:
        String pretty = event.prettyPrint()
        then:
        pretty.toString().trim() == '''
|-> test (0) : 010ms [001ms]
|     |-> child2 (0) : 005ms
|     |-> child1 (0) : 004ms'''.toString().trim()
    }

    def "demonstrate how much time is unaccounted for within its concurrent children"() {
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 4, isConcurrent: true)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 5, isConcurrent: true)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)

        when:
        String pretty = event.prettyPrint()
        then:
        pretty
        pretty.toString().trim() == '''
|-> test (0) : 010ms [005ms]
|     ||-> child2 (0) : 005ms
|     ||-> child1 (0) : 004ms'''.toString().trim()
    }

    def "demonstrate how much time is unaccounted for within its concurrent children and synchronous children"() {
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 4, isConcurrent: false)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 5, isConcurrent: true)
        ProfileEvent child3 = new ProfileEvent(name: "child3", runtimeInMillis: 5, isConcurrent: true)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)
        event.addChild(child3)

        when:
        String pretty = event.prettyPrint()
        then:
        pretty
        pretty.toString().trim() == '''
|-> test (0) : 010ms [001ms]
|     ||-> child2 (0) : 005ms
|     ||-> child3 (0) : 005ms
|     |-> child1 (0) : 004ms'''.toString().trim()
    }

    def "demonstrate how much time is unaccounted for within its concurrent children and several synchronous children"() {
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 4, isConcurrent: true)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 5, isConcurrent: false)
        ProfileEvent child3 = new ProfileEvent(name: "child3", runtimeInMillis: 6, isConcurrent: true)
        ProfileEvent child4 = new ProfileEvent(name: "child4", runtimeInMillis: 7, isConcurrent: false)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 20)
        event.addChild(child1)
        event.addChild(child2)
        event.addChild(child3)
        event.addChild(child4)

        when:
        String pretty = event.prettyPrint()
        then:
        pretty
        pretty.toString().trim() == '''
|-> test (0) : 020ms [002ms]
|     |-> child4 (0) : 007ms
|     |-> child2 (0) : 005ms
|     ||-> child3 (0) : 006ms
|     ||-> child1 (0) : 004ms'''.toString().trim()
    }


    def "demonstrate how much time is unaccounted for within its remote children"() {
        ProfileEvent child1 = new ProfileEvent(name: "child1", runtimeInMillis: 4, isRemote: true)
        ProfileEvent child2 = new ProfileEvent(name: "child2", runtimeInMillis: 5, isRemote: true)
        ProfileEvent event = new ProfileEvent(name: "test", runtimeInMillis: 10)
        event.addChild(child1)
        event.addChild(child2)

        when:
        String pretty = event.prettyPrint()
        then:
        pretty
        pretty.toString().trim() == '''
|-> test (0) : 010ms [005ms]
|     |||-> child2 (0) : 005ms
|     |||-> child1 (0) : 004ms'''.toString().trim()
    }
}
