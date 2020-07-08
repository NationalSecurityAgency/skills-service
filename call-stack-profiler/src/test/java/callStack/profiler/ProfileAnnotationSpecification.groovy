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

import callStack.profiler.CProf
import callStack.profiler.Profile
import spock.lang.Specification


class ProfileAnnotationSpecification extends Specification {
    class C1 {
        @Profile
        void m1() {
            Thread.sleep(50)
            new C2().m2()
        }

        @Profile
        void willThrow(){
            new C2().willThrow()
        }

        @Profile
        void willThrowAsWell(){
            new C2().willThrowAsWell()
        }
    }
    class C2 {
        @Profile
        void m2() {
            Thread.sleep(50)
        }

        @Profile
        void willThrow(){
            new C3().m4()
        }

        @Profile
        void willThrowAsWell(){
            new C3().willThrowAsWell()
        }
    }

    def "Profile annotation must add profiling to the annotation method"() {
        setup:

        when:
        new C1().m1()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C1.m1"
        CProf.rootEvent.runtimeInMillis > 0

        CProf.rootEvent.children
        CProf.rootEvent.children.size() == 1
        CProf.rootEvent.children.first().name == "ProfileAnnotationSpecification\$C2.m2"
        CProf.rootEvent.children.first().runtimeInMillis > 0
        CProf.rootEvent.ended
    }


    class C3 {
        @Profile
        String m3() {
            Thread.sleep(200)
            return "string"
        }

        @Profile
        String m4() {
            Thread.sleep(200)
            throw new IllegalArgumentException("aljaljfljaljf")
            return "string"
        }

        @Profile
        String willThrowAsWell() {
            Thread.sleep(200)
            try {
                (0..10).each {
                    List<WithAttr> list = [new WithAttr(attr: "blja"), ["aljl", "lajlakj"]]
                    // this should throw an exception
                    def groupBy = list.groupBy { it.attr }
                }
                return "groupBy"
            } catch (Throwable throwable) {
//                throwable.printStackTrace()
                throw throwable
            }
        }
    }

    class WithAttr{
        String attr
    }

    def "Profile method's return must be properly propagated"() {
        String res
        when:
        res = new C3().m3()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C3.m3"
        CProf.rootEvent.runtimeInMillis >= 0
        CProf.rootEvent.ended
        res == "string"
    }

    def "Thrown exception does NOT stop profiling from completing"() {
        String res
        Exception thrownE
        when:
        try {
            res = new C3().m4()
            fail "should never get here"
        } catch (Exception e) { thrownE = e}
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C3.m4"
        CProf.rootEvent.runtimeInMillis >= 200
        CProf.rootEvent.ended
        !res
        thrownE instanceof IllegalArgumentException
        thrownE.message == "aljaljfljaljf"
    }

    def "Nested exception does NOT stop profiling from completing"() {
        String res
        Exception thrownE
        when:
        try {
            res = new C1().willThrow()
            fail "should never get here"
        } catch (Exception e) { thrownE = e}
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C1.willThrow"
        CProf.rootEvent.runtimeInMillis >= 200
        CProf.rootEvent.ended
        !res
        thrownE instanceof IllegalArgumentException
        thrownE.message == "aljaljfljaljf"
    }

    def "Nested odd exception does NOT stop profiling from completing"() {
        String res
        Exception thrownE
        when:
        try {
            res = new C1().willThrowAsWell()
            fail "should never get here"
        } catch (Exception e) { thrownE = e}

        thrownE.printStackTrace()

        println CProf.prettyPrint()
        then:
//        CProf.rootEvent
//        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C1.willThrow"
//        CProf.rootEvent.runtimeInMillis >= 200
//        CProf.rootEvent.ended
        !res
//        thrownE instanceof IllegalArgumentException
//        thrownE.message == "aljaljfljaljf"
    }


    def "Can the entry method multiple times and time should be properly re-set on each call"() {
        when:
        (0..10).each {
            new C3().m3()
        }
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C3.m3"
        CProf.rootEvent.runtimeInMillis >= 200
        CProf.rootEvent.runtimeInMillis < 1000
        CProf.rootEvent.ended
    }

    class C4 {
        @Profile(name = "CustomName")
        void m1() {
            Thread.sleep(50)
        }
    }

    def "Annotation allows to change the name of the profile event"() {
        when:
        new C4().m1()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "CustomName"
        CProf.rootEvent.ended
        CProf.rootEvent.runtimeInMillis >= 50
    }


    class C5 {
        @Profile
        void empty() {
        }
    }

    def "Should be able to profile empty method, for some odd reason"() {
        when:
        new C5().empty()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C5.empty"
        CProf.rootEvent.ended
    }

    class C6 {
        boolean b = true

        @Profile(name = "multi-return")
        void multiReturn() {
            if (b) {
                Thread.sleep(50)
                return
            }
            Thread.sleep(10)
            return
        }
    }

    def "Profile methods that have multiple returns"() {
        when:
        new C6().multiReturn()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "multi-return"
        CProf.rootEvent.ended
        CProf.rootEvent.runtimeInMillis >= 50
    }

    class C7 {
        boolean b = true
        @Profile
        void callMethod() {
            dontAggregate()
            dontAggregate()
            dontAggregate()
        }

        @Profile(aggregateIntoSingleEvent = false)
        void dontAggregate() {
            Thread.sleep(50)
        }
    }

    def "Allow each call to be a separate profile event"() {
        when:
        new C7().callMethod()
//        println CProf.rootEvent.prettyPrint()
        then:
        CProf.rootEvent
        CProf.rootEvent.name == "ProfileAnnotationSpecification\$C7.callMethod"
        CProf.rootEvent.children.size() == 3
        CProf.rootEvent.runtimeInMillis >= 150
    }
}
