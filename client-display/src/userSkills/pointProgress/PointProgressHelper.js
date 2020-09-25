/*
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
export default {
    calculateXAxisMaxTimestamp(histResult) {
        // only perform this calculation if there are at least 2 months of points
        if (!histResult || !histResult.pointsHistory || histResult.pointsHistory.length < 60) {
            return null;
        }
        const pointHistory = histResult.pointsHistory;
        const maxAchievedTimeStamp = this.getMaxAchievedTimestamp(histResult);

        // start at the end
        let resPosition = pointHistory.length - 1;
        const maxPoints = pointHistory[resPosition].points;

        // tolerate 2% of point gained
        const pointsThreshold = Math.trunc(maxPoints * 0.98);
        for (let i = resPosition; i > 30; i -= 1) {
            resPosition = i;
            const pointsToCompare = pointHistory[i].points;
            if (pointsThreshold > pointsToCompare) {
                break;
            }
        }
        let maxTimestampRes = new Date(pointHistory[resPosition].dayPerformed).getTime();
        if (maxAchievedTimeStamp) {
            maxTimestampRes = Math.max(maxTimestampRes, maxAchievedTimeStamp);
        }
        return maxTimestampRes;
    },
    getMaxAchievedTimestamp(histResult) {
        let maxAchievedTimeStamp = -1;
        if (histResult.achievements && histResult.achievements.length > 0) {
            const timestamps = histResult.achievements.map((item) => new Date(item.achievedOn).getTime());
            maxAchievedTimeStamp = Math.max(...timestamps);
        }
        return maxAchievedTimeStamp;
    },
};
