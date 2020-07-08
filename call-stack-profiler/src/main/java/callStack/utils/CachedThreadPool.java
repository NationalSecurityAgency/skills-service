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
package callStack.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CachedThreadPool extends AbstractThreadPool {

	public CachedThreadPool(String name) {
		m_pool = Executors.newCachedThreadPool(new NamedThreadFactory(name));
	}

	public CachedThreadPool(final String name, int minNumOfThreads, int maxNumOfThreads) {
		m_pool = new ThreadPoolExecutor(minNumOfThreads, maxNumOfThreads,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory(name));
	}
}
