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
package skills.notify

import groovy.transform.PackageScope
import org.slf4j.Logger
import skills.storage.model.Notification

@PackageScope
class DispatchState {
    int count = 0
    int errCount = 0
    String lastErrMsg
    String prepend
    Logger wrappedLog

    /**
     * Executes the specified closure and only prints errors if they differ from the previous error
     * @param notification
     * @param doer
     * @return
     */
    boolean doWithErrHandling(Notification notification, Closure doer) {
        try {
            doer()
            count++
            return true
        } catch (Throwable t) {
            // don't print the same message over and over again
            if (!lastErrMsg?.equalsIgnoreCase(t.message)) {
                wrappedLog.error("${prepend}Failed to send notification with id [${notification.id}] and type [${notification.type}]. Updating notification to retry", t)
                lastErrMsg = t.message
            }
            errCount++
            return false
        }
    }

}
