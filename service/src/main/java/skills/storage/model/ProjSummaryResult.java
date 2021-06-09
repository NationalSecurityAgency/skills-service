/**
 * Copyright 2021 SkillTree
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
package skills.storage.model;

import java.util.Date;

public interface ProjSummaryResult {
    public abstract String getProjectId();

    public abstract String getName();

    public abstract int getTotalPoints();

    public abstract int getNumSubjects();

    public abstract int getNumSkills();

    public abstract int getNumBadges();

    public abstract int getNumErrors();

    public abstract Date getCreated();

    public abstract Date getLastReportedSkill();

    public abstract Date getExpirationTriggered();

    public abstract Boolean getExpiring();
}
