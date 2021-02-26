package skills.utils

import groovy.lang.Closure

class WaitFor {

    static boolean wait(Closure closure) {
        wait(60, closure)
    }

    static boolean wait(int secsToWait, Closure closure) {
        long start = System.currentTimeMillis()
        while(!closure.call() && (System.currentTimeMillis() - start) < (secsToWait * 1000) ) {
            Thread.sleep(250)
        }

        return closure.call()
    }
}
