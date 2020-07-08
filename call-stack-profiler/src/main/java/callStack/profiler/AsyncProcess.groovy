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

import groovy.util.logging.Slf4j
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
class AsyncProcess {

    int secondsToPoll = 2
    int queueSize = 1000
    boolean dropIfFull = false

    BlockingQueue<Closure> toProcess
    Thread thread
    AtomicBoolean stopped = new AtomicBoolean(false)

    AsyncProcess start(){
        assert !toProcess, "already started"

        toProcess = new ArrayBlockingQueue<Closure>(queueSize)
        log.info("Starting with queue size of [{}]", queueSize)
        thread = Thread.start("${this.class.simpleName}(${this.hashCode()})") {
            while(!stopped.get()){
                Closure closure = toProcess.poll(secondsToPoll, TimeUnit.SECONDS)
                try {
                    if (closure != null) {
                        closure.call()
                    }
                } catch (Throwable t){
                    log.error("Failed to process async task", t)
                }
            }
        }
       return this
    }

    boolean async(Closure executeMe ){
        assert toProcess != null
        boolean res = true
        if(dropIfFull){
            res = toProcess.offer(executeMe)
            if(!res){
                log.warn("Async queue is full!!!! \n" +
                        "    Investigate why internal thread isn't servicing requests in a timely manner.\n" +
                        "    Dropping incoming request for class [{}]", executeMe.class)
            }
        } else {
            toProcess.add(executeMe)
        }

        return res
    }

    void stop() {
        stopped.set(true)
    }
}
