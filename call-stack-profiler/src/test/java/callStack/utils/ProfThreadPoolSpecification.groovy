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
package callStack.utils

import callStack.profiler.CProf
import callStack.profiler.ProfThreadPool
import callStack.utils.ThreadPoolUtils
import spock.lang.Specification

import java.util.concurrent.Future

class ProfThreadPoolSpecification extends Specification {


    String poolName = "pool"
    ProfThreadPool profThreadPool = new ProfThreadPool(poolName, 2, 5)
    def setup() {
        CProf.clear()
    }

    def "Thread pool usage where underlying exec does NOT name events"() {
        List<String> res
        when:
        CProf.prof("l1") {
            res = profThreadPool.asyncExec([
                    ThreadPoolUtils.callable { return "1" },
                    ThreadPoolUtils.callable { return "2" }
            ])
        }

        then:
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")
        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 2
        CProf.rootEvent.children.collect({it.name}).sort() == ["$poolName-1", "$poolName-2"]
    }



    def "Thread pool allows to assign unique name to each event"() {
        ProfThreadPool profThreadPool = new ProfThreadPool(poolName, 2, 5)
        profThreadPool.assignUniqueNameToEachRootEvent = true

        List<String> res
        when:
        CProf.prof("l1") {
            res = profThreadPool.asyncExec([
                    ThreadPoolUtils.callable { return "1" },
                    ThreadPoolUtils.callable { return "2" }
            ])
        }

        println CProf.prettyPrint()

        then:
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")
        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 2
        List<String> names = CProf.rootEvent.children.collect({it.name}).sort()
        names.first().startsWith("$poolName-1")
        names.last().startsWith("$poolName-2")
    }


    def "Threadpool shouldn't fail if there is not profile event on the parent thread"() {
        List<String> res
        when:
        res = profThreadPool.asyncExec([
                ThreadPoolUtils.callable { return "1" },
                ThreadPoolUtils.callable { return "2" }
        ])
        then:
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")
        !CProf.rootEvent
    }


    def "Thread pool usage where underlying exec name events"() {
        List<String> res
        when:
        CProf.prof("l1") {
            res = profThreadPool.asyncExec([
                    ThreadPoolUtils.callable { CProf.prof("1") {}; return "1" },
                    ThreadPoolUtils.callable { CProf.prof("2") {}; return "2" }
            ])
        }
        then:
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")
//        println CProf.rootEvent.prettyPrint()
        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 2
        List<String> names = CProf.rootEvent.children.collect({ it.name }).sort()
        names.find({ it.startsWith("1-pool") })
        names.find({ it.startsWith("2-pool") })
    }

    def "Thread pool usage in multi-level call stack"() {
        List<String> res
        when:
        CProf.prof("l1") {
            CProf.prof("l2") {
                res = profThreadPool.asyncExec([
                        ThreadPoolUtils.callable { CProf.prof("l3") { CProf.prof("l4") { CProf.prof("l5") {} } }; return "1" },
                        ThreadPoolUtils.callable { CProf.prof("l3") { CProf.prof("l4") {} }; return "2" }
                ])
            }
        }
        then:
//        println CProf.rootEvent.prettyPrint()
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")

        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 1
        CProf.rootEvent.children.first().name == "l2"
        CProf.rootEvent.children.first().children.size() == 2
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" })
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().name == "l4"
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.first().name == "l5"
        !CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.first().children

        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" })
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.first().name == "l4"
        !CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.first().children
    }

    def "can use futures with the pool"() {
        List<String> res = []
        when:
        CProf.prof("l1") {
            Future<String> futureRes = profThreadPool.submit(ThreadPoolUtils.callable { return "1" })
            Future<String> futureRes1 = profThreadPool.submit(ThreadPoolUtils.callable { return "2" })

            res.add(futureRes.get())
            res.add(futureRes1.get())
        }
        println CProf.rootEvent.prettyPrint()
        then:
        res
        res.size() == 2
        res.sort() == ["1", "2"]
        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 2
    }

    def "Thread pool usage in multi-level call stack via futures"() {
        List<String> res = []
        when:
        CProf.prof("l1") {
            CProf.prof("l2") {
                Future f1 = profThreadPool.submit(ThreadPoolUtils.callable { CProf.prof("l3") { CProf.prof("l4") { CProf.prof("l5") {} } }; return "1" })
                Future f2 = profThreadPool.submit( ThreadPoolUtils.callable { CProf.prof("l3") { CProf.prof("l4") {} }; return "2" })
                res.add(f1.get())
                res.add(f2.get())
            }
        }
        then:
        println CProf.rootEvent.prettyPrint()
        res
        res.size() == 2
        res.contains("1")
        res.contains("2")

        CProf.rootEvent.name == "l1"
        CProf.rootEvent.children.size() == 1
        CProf.rootEvent.children.first().name == "l2"
        CProf.rootEvent.children.first().children.size() == 2
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" })
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().name == "l4"
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.first().name == "l5"
        !CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-1" }).children.first().children.first().children

        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" })
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.size() == 1
        CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.first().name == "l4"
        !CProf.rootEvent.children.first().children.find({ it.name == "l3-pool-2" }).children.first().children
    }


    def "Warn when usage reaches high percentage"() {
        ProfThreadPool threadPool = new ProfThreadPool(poolName, 5, 5)
        List<Future> futures = []
        when:
        (0..4).each {
            futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        }


        (0..2).each {
            threadPool.asyncExec([ThreadPoolUtils.callable { Thread.sleep(500) }])
        }

        futures.each {
            it.get()
        }
        then:
        true

        // nothing go validate, look for warn messages
    }


    def "Do not warn after the pool went below threshold"() {
        ProfThreadPool threadPool = new ProfThreadPool(poolName, 5)
        List<Future> futures = []
        when:
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))

        futures.each {
            it.get()
        }
        futures.clear()

        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.add(threadPool.submit(ThreadPoolUtils.callable { (0..2).each{Thread.sleep(500)} }))
        futures.each {
            it.get()
        }

        then:
        true

        // nothing go validate, look for warn messages
    }

}

