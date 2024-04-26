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
import Vue from 'vue';

const second = 1000;
const minute = second * 60;
const hour = minute * 60;

const simpleClockFilter = (valueInMs) => {
    if (valueInMs === null || valueInMs === undefined) {
        return 'N/A';
    }
    if (valueInMs < second) {
        return '00:00:00';
    }
    if (valueInMs < minute) {
        const seconds = Math.trunc(valueInMs / second);
        return `00:00:${seconds.toString().padStart(2, '0')}`;
    }
    if (valueInMs < hour) {
        const totalSeconds = Math.trunc(valueInMs / second);
        const minutes = Math.trunc(totalSeconds / 60);
        const seconds = totalSeconds - (minutes * 60);
        return `00:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
    const totalMinutes = Math.trunc(valueInMs / minute);
    const hours = Math.trunc(totalMinutes / 60);
    const minutes = totalMinutes - (hours * 60);
    const totalSeconds = Math.trunc(valueInMs / second);
    const seconds = totalSeconds - (hours * 60 * 60) - (minutes * 60);
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
};
Vue.filter('clock', simpleClockFilter);

export default simpleClockFilter;
