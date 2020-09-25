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
var moment = require('moment-timezone');
import PointProgressHelper from '@/userSkills/pointProgress/PointProgressHelper';

describe('PointProgressHelper', () => {
    it('null for null', () => {
        const res = PointProgressHelper.calculateXAxisMaxTimestamp(null);
        expect(res).toBeNull();

        const res1 = PointProgressHelper.calculateXAxisMaxTimestamp({ pointHistory: null });
        expect(res1).toBeNull();
    });

    it('only calculate max date if num days >= 60', () => {
        const pointsHistoryList = [];
        for (let i = 0; i < 59; i += 1) {
            pointsHistoryList.push(
                {
                    dayPerformed: '2020-09-02T00:00:00.000+00:00',
                    points: 100,
                },
            );
        }

        const data = {
            pointsHistory: pointsHistoryList,
            achievements: [],
        };

        const res = PointProgressHelper.calculateXAxisMaxTimestamp(data);
        expect(res).toBeNull();

        pointsHistoryList.push(
            {
                dayPerformed: '2020-09-02T00:00:00.000+00:00',
                points: 100,
            },
        );
        const res1 = PointProgressHelper.calculateXAxisMaxTimestamp(data);
        expect(res1).toEqual(new Date(pointsHistoryList[0].dayPerformed).getTime());
    });

    it('do not go before max achievement date', () => {
        const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
        const pointsHistoryList = [];
        for (let i = 0; i < 120; i += 1) {
            const day = m.clone().add(i, 'day').tz('UTC').format();
            pointsHistoryList.push(
                {
                    dayPerformed: day,
                    points: 100,
                },
            );
        }

        const withoutAchievementRes = PointProgressHelper.calculateXAxisMaxTimestamp({
            pointsHistory: pointsHistoryList,
            achievements: [],
        });
        expect(withoutAchievementRes).toEqual(new Date(pointsHistoryList[31].dayPerformed).getTime());

        const withAchievementRes = PointProgressHelper.calculateXAxisMaxTimestamp({
            pointsHistory: pointsHistoryList,
            achievements: [{
                achievedOn: pointsHistoryList[50].dayPerformed,
                points: 500,
                name: 'Levels 1',
            }, {
                achievedOn: pointsHistoryList[65].dayPerformed,
                points: 500,
                name: 'Levels 2',
            }],
        });
        expect(withAchievementRes).toEqual(new Date(pointsHistoryList[65].dayPerformed).getTime());
    });

    it('find the max date by comparing points', () => {
        const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
        const pointsHistoryList = [];
        for (let i = 0; i < 120; i += 1) {
            const day = m.clone()
                .add(i, 'day')
                .tz('UTC')
                .format();
            pointsHistoryList.push(
                {
                    dayPerformed: day,
                    points: i < 100 ? 100 : 200,
                },
            );
        }

        const res = PointProgressHelper.calculateXAxisMaxTimestamp({
            pointsHistory: pointsHistoryList,
            achievements: [],
        });
        expect(res).toEqual(new Date(pointsHistoryList[99].dayPerformed).getTime());
    });

    it('tolerate up to 2% difference in points', () => {
        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
        const pointsHistoryList = [];
        for (let i = 0; i < 120; i += 1) {
            const day = m.clone()
                .add(i, 'day')
                .tz('UTC')
                .format();
            let points = 100;
            if (i > 105) {
                points = 200;
            } else if (i > 100) {
                points = 198;
            } else if (i > 90) {
                points = 196;
            } else if (i > 80) {
                points = 195;
            }

            pointsHistoryList.push(
                {
                    dayPerformed: day,
                    points,
                },
            );
        }

        const res = PointProgressHelper.calculateXAxisMaxTimestamp({
            pointsHistory: pointsHistoryList,
            achievements: [],
        });
        expect(res).toEqual(new Date(pointsHistoryList[90].dayPerformed).getTime());
    });
});
