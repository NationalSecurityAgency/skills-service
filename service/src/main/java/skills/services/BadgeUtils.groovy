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
package skills.services

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.skillLoading.model.SkillBadgeSummary
import skills.storage.model.SkillDefMin
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillDefWithExtra

class BadgeUtils {

    public static boolean withinActiveTimeframe(SkillDefMin skillDef) {
        boolean withinActiveTimeframe = true;
        if (skillDef.startDate && skillDef.endDate) {
            Date now = new Date()
            withinActiveTimeframe = skillDef.startDate.before(now) && skillDef.endDate.after(now)
        }
        return withinActiveTimeframe
    }

    public static boolean withinActiveTimeframe(SkillDefParent skillDef) {
        boolean withinActiveTimeframe = true;
        if (skillDef.startDate && skillDef.endDate) {
            Date now = new Date()
            withinActiveTimeframe = skillDef.startDate.before(now) && skillDef.endDate.after(now)
        }
        return withinActiveTimeframe
    }

    public static boolean withinActiveTimeframe(SkillBadgeSummary skillDef) {
        boolean withinActiveTimeframe = true;
        if (skillDef.startDate && skillDef.endDate) {
            Date now = new Date()
            withinActiveTimeframe = skillDef.startDate.before(now) && skillDef.endDate.after(now)
        }
        return withinActiveTimeframe
    }

    public static boolean afterStartTime(SkillDefWithExtra skillDef) {
        boolean withinActiveTimeframe = true;
        if (skillDef.startDate) {
            Date now = new Date()
            withinActiveTimeframe = skillDef.startDate.before(now)
        }
        return withinActiveTimeframe
    }

    public static boolean shouldRollOff(SkillBadgeSummary skillDef, Integer daysToRollOff) {
        if(!withinActiveTimeframe(skillDef)) {
            boolean shouldRollOff = false
            use(TimeCategory) {
                Date now = new Date()
                Date rollOffDate = skillDef.endDate + daysToRollOff.days
                if(now.after(rollOffDate)) {
                    shouldRollOff = true
                }
            }

            return shouldRollOff
        }
        return false
    }

}
