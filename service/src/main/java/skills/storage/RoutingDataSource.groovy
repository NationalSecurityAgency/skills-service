/**
 * Copyright 2025 SkillTree
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
package skills.storage

import groovy.util.logging.Slf4j
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

import javax.sql.DataSource

@Slf4j
class RoutingDataSource extends AbstractRoutingDataSource {

    RoutingDataSource(DataSource writer, DataSource reader) {
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("writer", writer);
        dataSources.put("reader", reader);

        setTargetDataSources(dataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String res = ReadOnlyDataSourceContext.isReadOnly() ? "reader" : "writer";

        if (log.isDebugEnabled()) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace()
            String skillsCallStack = trace.findAll { it.toString().contains("skills.")}.join( "<--")
            boolean isEmailDispatch = trace.findAll { it.toString().contains('EmailNotifier.dispatchNotifications') }
            boolean isAsyncScheduler = trace.find { it.className.contains("kagkarlsson")}?.className
            log.debug("Using [{}] datasource, isAsyncScheduler: [{}], isEmailDispatch: [{}], skillsCallStack = {}", res, isAsyncScheduler, isEmailDispatch, skillsCallStack)
        }

        return res
    }
}
