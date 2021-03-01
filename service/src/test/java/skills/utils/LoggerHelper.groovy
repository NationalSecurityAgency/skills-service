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
