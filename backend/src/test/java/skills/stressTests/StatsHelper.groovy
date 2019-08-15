package skills.stressTests

import groovy.util.logging.Slf4j

import java.util.concurrent.atomic.AtomicLong

@Slf4j
class StatsHelper {

    AtomicLong totalEvents = new AtomicLong(0)
    AtomicLong totalTime = new AtomicLong(0)

    AtomicLong last1kTime = new AtomicLong(0)

    ThreadLocal<Long> startTime = new ThreadLocal<>()

    Map<String, Long> binExecTimes = [:]
    Map<String, Long> binExecTimesLast1k = [:]

    void startEvent() {
        startTime.set(System.currentTimeMillis())
    }

    synchronized void endEvent() {
        long execTime = System.currentTimeMillis() - startTime.get()
        addToBinnedExecTimes(binExecTimes, execTime)
        addToBinnedExecTimes(binExecTimesLast1k, execTime)
        int totalEvents = totalEvents.incrementAndGet()
        long totalExecTime = totalTime.addAndGet(execTime)
        long last1kExecTime = last1kTime.addAndGet(execTime)

        if (totalEvents % 1000 == 0) {
            log.info(buildMessage(totalEvents, totalExecTime, last1kExecTime))
            last1kTime.set(0)
            binExecTimesLast1k.clear()
        }
    }

    void addToBinnedExecTimes(Map<String, Long> map, long execTime) {
        if ( execTime <= 50 ){
            incrementBinnedCount(map, "a) time <= 50")
        } else if (execTime > 50 && execTime <= 100) {
            incrementBinnedCount(map, "b)  50 < time <= 100")
        } else if (execTime > 100 && execTime <= 150) {
            incrementBinnedCount(map, "c) 100 < time <= 150")
        } else if (execTime > 150 && execTime <= 200) {
            incrementBinnedCount(map, "d) 150 < time <= 200")
        } else if (execTime > 200 && execTime <= 300) {
            incrementBinnedCount(map, "e) 200 < time <= 300")
        } else if (execTime > 300 && execTime <= 1000) {
            incrementBinnedCount(map, "f) 300 < time <= 1s")
        }else {
            incrementBinnedCount(map, "g) time > 1s")
        }
    }

    void incrementBinnedCount(Map<String, Long> map, String name){
        Long count = map[name]
        if ( count == null ){
            map[name] = 1
        } else {
            map[name] = count + 1l
        }
    }

    private String buildMessage(totalEvents, totalExecTime, last1kExecTime) {
        List<String> msgs = [
                "\n--------------------------------",
                "Total Events: [${totalEvents}]",
                "Avg. Event Response: [${(totalEvents / (totalExecTime /1000)).trunc(2)}] ms",
                "Avg. Event Response (last 1k events): [${(1000 / (last1kExecTime /1000)).trunc(2)}] ms",

        ]

        msgs.add("Overall breakdown:")
        msgs.addAll(binExecTimes.collect { "  ${it.key}: ${it.value}" }.sort())
        msgs.add("Last 1k breakdown:")
        msgs.addAll(binExecTimesLast1k.collect { "  ${it.key}: ${it.value}" }.sort())

        return msgs.join("\n")
    }
}
