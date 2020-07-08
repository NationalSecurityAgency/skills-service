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

import groovy.transform.CompileStatic

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@CompileStatic
class ProfFuture<T> implements Future<T> {

    Future<ProfAsyncResult<T>> underlyingFuture

    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        return underlyingFuture.cancel(mayInterruptIfRunning)
    }

    @Override
    boolean isCancelled() {
        return underlyingFuture.cancelled
    }

    @Override
    boolean isDone() {
        return underlyingFuture.done
    }

    @Override
    T get() throws InterruptedException, ExecutionException {
        ProfAsyncResult<T> profAsyncResult = underlyingFuture.get()
        documentProfiling(profAsyncResult)
        return profAsyncResult.res
    }

    @Override
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ProfAsyncResult<T> profAsyncResult = underlyingFuture.get(timeout,unit)
        documentProfiling(profAsyncResult)
        return profAsyncResult.res
    }

    private void documentProfiling(ProfAsyncResult<T> profAsyncResult) {
        profAsyncResult.profileEvent.concurrent = true
        if (CProf?.parent) {
            CProf?.parent.addChild(profAsyncResult.profileEvent)
        }
    }
}
