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
package skills.utils

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory

class LoggerHelper {

    private final ListAppender<ILoggingEvent> listAppender
    private long start

    LoggerHelper(Class<?> clazz) {
        assert clazz
        Logger clazzLogger = (Logger) LoggerFactory.getLogger(clazz);
        listAppender = new ListAppender<>();
        listAppender.start();
        start = System.currentTimeMillis()
        clazzLogger.addAppender(listAppender)
    }

    List<ILoggingEvent> getLogEvents() {
        return listAppender.list.findAll { it.timeStamp > start }
    }

    boolean hasLogMsgStartsWith(String str) {
        return getLogEvents().find { it.message.startsWith("Dispatched ") }
    }

    void stop() {
        listAppender.stop()
    }
}
