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
import format from 'number-format.js';
import dayjs from '../DayJsCustomizer';

const timeFromNowFormatter = (value) => dayjs(value).startOf('seconds').fromNow();

const numberFormatter = (value, fractionSize) => {
    let formatString = '#,##0.';
    if (fractionSize || fractionSize === 0) {
        formatString = `${formatString}${'0'.repeat(fractionSize)}`;
    }
    const fv = parseFloat(value);
    return format(formatString, fv);
};

const FiltersPlugin = (vue) => {
    vue.filter('formatDate', (dateStr, fstr) => {
        if (!dateStr) return '';
        return dayjs(dateStr).format(fstr);
    });
    vue.filter('relativeTime', (date) => dayjs(date).fromNow());
    vue.filter('timeFromNow', timeFromNowFormatter);
    vue.filter('number', numberFormatter);
};

export default FiltersPlugin;
