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
import callStack.profiler.ProfileEvent
import org.apache.commons.lang3.time.StopWatch
import spock.lang.Specification

class CProfSpecification extends Specification {


    def setup() {
        CProf.clear()
    }


    class C1 {
        C2 c2 = new C2()

        void callC2(boolean disableTree) {
            CProf.prof("callC2") {
                c2.callC3(disableTree)
            }
        }
    }

    class C2 {
        C3 c3 = new C3()

        void callC3(boolean disableTree) {
            CProf.prof("callC3") {
                Thread.sleep(100)
                c3.some(disableTree)
                c3.m2(disableTree)
            }
        }
    }

    class C3 {
        void some(boolean disableTree) {
            CProf.prof("some") {
                Thread.sleep(150)
            }

            CProf.prof("anotherSome") {
                Thread.sleep(200)
            }
        }

        void m2(boolean disableTree) {
            CProf.prof("m2") {
                Thread.sleep(50)
                CProf.turnTreeProfilingOff.set(disableTree)
            }

            CProf.prof("m3") {
                Thread.sleep(350)
            }

            (0..5).each {
                CProf.prof("forLoop") {
                    Thread.sleep(50)
                }
            }
        }
    }

    class C8 {

        @Profile
        void m2() {

            Thread.sleep(50)
            m2("blah")

        }
        @Profile
        void m2(String s) {

            Thread.sleep(50)

        }

    }

    def "call stack should propagate exceptions"() {
        when:
        new ThrowExceptionClass().profAndThrow()
        then:
        thrown(RuntimeException)
    }

    def "Test Simple Hierarchy Profiling"() {
        setup:
        C1 c1 = new C1()
        ProfileEvent event
        when:
        c1.callC2(false)
        then:
        CProf.rootEvent.name == "callC2"
        CProf.rootEvent.runtimeInMillis >= 1000
        CProf.rootEvent.children.size() == 1
        CProf.rootEvent.children.first().name == "callC3"
        CProf.rootEvent.children.first().runtimeInMillis >= 1000
        CProf.rootEvent.children.first().children.size() == 5
        Map<String, List<ProfileEvent>> eventsByName = CProf.rootEvent.children.first().children.groupBy { it.name }
        eventsByName["some"].first().runtimeInMillis >= 150
        eventsByName["anotherSome"].first().runtimeInMillis >= 200
        eventsByName["m2"].first().runtimeInMillis >= 50
        eventsByName["m3"].first().runtimeInMillis >= 350
        eventsByName["forLoop"].first().runtimeInMillis >= (50 * 5)
        eventsByName["forLoop"].first().numOfInvocations == 6
    }

    def "Test Simple Hierarchy Profiling - tree profiling disabled mid profiling"() {
        setup:
        C1 c1 = new C1()
        ProfileEvent event
        when:
        c1.callC2(true)
        CProf.turnTreeProfilingOff.set(false)
        then:
        CProf.rootEvent.name == "callC2"
//        println CProf.prettyPrint()
    }

    def "Test Simple Hierarchy Profiling - tree profiling disabled"() {
        setup:
        C4 c4 = new C4()
        ProfileEvent event
        when:
        CProf.turnTreeProfilingOff.set(true)
        c4.m()
        CProf.turnTreeProfilingOff.set(false)
        then:
        CProf.rootEvent.name == "root"
        CProf.rootEvent.children.size() == 0
//        println CProf.prettyPrint()
    }


    class C4 {
        void m() {
            CProf.prof("root") {
                (0..5).each {
                    CProf.prof("call", false) {
                        Thread.sleep(50)
                    }
                }
            }
        }
    }

    def "Allow for events to not be aggregated on the same hierarchy level with the same name"() {
        setup:
        C4 c4 = new C4()
        ProfileEvent event
        when:
        c4.m()
//        println CProf.prettyPrint()
        then:
        CProf.rootEvent.name == "root"
        CProf.rootEvent.runtimeInMillis >= 300
        CProf.rootEvent.children.size() == 6
    }


    class C5 {
        void m() {
            CProf.prof("root") {
                (0..5).each {
                    CProf.start("call")
                    Thread.sleep(50)
                    CProf.stop("call", false)
                }
            }
        }
    }

    def "Allow for events to not be aggregated on the same hierarchy level with the same name - use stopProf Method"() {
        setup:
        C5 c5 = new C5()
        ProfileEvent event
        when:
        c5.m()
//        println CProf.prettyPrint()
        then:
        CProf.rootEvent.name == "root"
        CProf.rootEvent.runtimeInMillis >= 300
        CProf.rootEvent.children.size() == 6
        CProf.rootEvent.children.first().name.startsWith("call")
    }

    def "Print out should look good for large hierarchy"() {
        setup:
        when:
        CProf.prof("l1") {
            CProf.prof("l2") {
                CProf.prof("l3") {
                    CProf.prof("l4") {
                        CProf.prof("l5") {
                            CProf.prof("l6") {
                                CProf.prof("l7") {
                                    CProf.prof("l8") {
                                        (0..10).each {
                                            CProf.prof("l9-${it}") {

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CProf.prof("l2-1") {
                (0..10).each {
                    CProf.prof("l3-${it}") {
                        CProf.prof("l4") {

                        }
                    }
                }
            }
            CProf.prof("l2-3") {
                CProf.prof("l3") {
                    CProf.prof("l4") {

                    }
                }
            }
        }
        then:
        CProf.prettyPrint()
//        println CProf.prettyPrint()
    }


    def "Profiling must be fast in a for-loop"() {
        when:
        CProf.prof("load") {}
        StopWatch watch = new StopWatch()
        watch.start()
        CProf.prof("top") {
            (0..1000).each {
                CProf.prof("forLoop") {
                }
            }
        }
        watch.stop()

//        println watch.time
//        println CProf.prettyPrint()
        then:
        watch.time < 1000
    }

    def "Pretty print large numbers"() {

        when:
        CProf.prof("pretty") {}
        CProf.rootEvent.numOfInvocations = 10000
        String prettyPrint = CProf.prettyPrint()
        then:
        prettyPrint.startsWith("|-> pretty (10,000) :")
    }


    def "Pretty print should not be too slow"() {
        setup:
        when:
        CProf.prof("l1") {
            CProf.prof("l2") {
                CProf.prof("l3") {
                    CProf.prof("l4") {
                        CProf.prof("l5") {
                            CProf.prof("l6") {
                                CProf.prof("l7") {
                                    CProf.prof("l8") {
                                        (0..10).each {
                                            CProf.prof("l9-${it}") {

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CProf.prof("l2-1") {
                (0..10).each {
                    CProf.prof("l3-${it}") {
                        CProf.prof("l4") {

                        }
                    }
                }
            }
            CProf.prof("l2-3") {
                CProf.prof("l3") {
                    CProf.prof("l4") {

                    }
                }
            }
        }
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        CProf.prettyPrint()
        stopWatch.stop()

//        println stopWatch.time
        then:
        CProf.prettyPrint()
//        println CProf.prettyPrint()
    }

    def "Call stack profiler should properly propagate exceptions"() {

        when:
        CProf.prof("with exception"){
            throw new IllegalArgumentException("fail")
        }
        then: thrown (IllegalArgumentException)

    }

    def "Should be able to handle overloaded methods when profiling is off"() {

        setup:
        CProf?.turnTreeProfilingOff.set(true)
        when:
        new C8().m2()
        then:
        CProf?.turnTreeProfilingOff.set(false)

    }

    static class ThrowExceptionClass {
        void profAndThrow() {
            CProf.prof("m1") {
                Thread.sleep(50)
                throwException("blah")
            }

        }

        void throwException(String s) {
            CProf.start("m2")
            Thread.sleep(50)
            throw new RuntimeException("Exception")
        }

    }

    def "ability to preserve root ProfileEvent object for later utilization"(){
        List<ProfileEvent> events = []

        when:
        2.times {
            CProf.prof("prof"){
                Thread.sleep(50)
            }
            events.add(CProf.rootEvent)
        }
        then:
        events.size() == 2
        events.first().hashCode() != events.last().hashCode()
    }


}
