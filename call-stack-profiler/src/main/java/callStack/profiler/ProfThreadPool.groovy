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

import callStack.utils.CachedThreadPool
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.Callable
import java.util.concurrent.Future

@Slf4j
@CompileStatic
class ProfThreadPool {

    CachedThreadPool cachedThreadPool
    boolean assignUniqueNameToEachRootEvent = false
    final String poolName
    boolean warnIfFull = true

    public ProfThreadPool(String name) {
        cachedThreadPool = new CachedThreadPool(name)
        this.poolName = name
    }

    public ProfThreadPool(final String name, int numThreads) {
        this(name, numThreads, numThreads)
    }

    public ProfThreadPool(final String name, int minNumOfThreads, int maxNumOfThreads) {
        cachedThreadPool = new CachedThreadPool(name, minNumOfThreads, maxNumOfThreads)
        this.poolName = name
    }

    static class ProfCallable implements Callable<ProfAsyncResult> {
        Callable callable
        boolean uniqueName = false

        @Override
        ProfAsyncResult call() throws Exception {
            CProf.clear()
            final String threadName = Thread.currentThread().name
            String rootEventName = threadName
            if (uniqueName) {
                rootEventName = "${rootEventName}-${UUID.randomUUID().toString()}"
            }
            Object o
            ProfileEvent rootEvent = CProf.prof(rootEventName) {
                o = callable.call()
            }

            // use the interinal impl root event if there is one defined
            if (rootEvent && rootEvent?.children.size() == 1) {
                rootEvent = rootEvent.children.first()
                rootEvent.name = rootEvent.name + "-" + threadName
            }
            return new ProfAsyncResult(res: o, profileEvent: rootEvent)
        }
    }

    public <T> List<T> asyncExec(List<Callable<T>> listToSubmit) {
        assert listToSubmit

        warnIfFull(listToSubmit.size())
        List<Callable<T>> profCallables = []
        listToSubmit.each {
            profCallables.add((Callable<T>) new ProfCallable(callable: it, uniqueName: assignUniqueNameToEachRootEvent))
        }

        List<ProfAsyncResult> profAsyncResults = (List<ProfAsyncResult>) cachedThreadPool.submitAndGetResults(profCallables)
        List<T> res = []
        profAsyncResults.each {
            res.add((T) it.res)
            it.profileEvent.concurrent = true
            if (CProf?.parent) {
                CProf?.parent.addChild(it.profileEvent)
            }
        }

        return res
    }

    private void warnIfFull(int numToSubmit) {
        if (warnIfFull) {
            double currentPoolSize = (double) (cachedThreadPool.activePoolSize + numToSubmit)
            double percentFull = currentPoolSize / cachedThreadPool.maximumPoolSize
            if (percentFull > 0.9) {
                log.warn("[{}] pool is > 90% full, [{}] current threads", poolName, ((int) currentPoolSize - 1))
            }
        }
    }

    public <T> Future<T> submit(Callable<T> callable) {
        warnIfFull(1)
        List<Future> futures = cachedThreadPool.submit(new ProfCallable(callable: callable, uniqueName: assignUniqueNameToEachRootEvent), 1)
        return new ProfFuture<T>(underlyingFuture: futures.first())
    }

    int getMaximumPoolSize() {
        return cachedThreadPool.maximumPoolSize
    }

    int getCurrentPoolSize() {
        return cachedThreadPool.currentPoolSize
    }

    int getActivePoolSize() {
        return cachedThreadPool.activePoolSize
    }

    void shutdown() {
        cachedThreadPool.shutdown()
    }
}
