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
import moment from 'moment';

const timeFromNowFormatter = (value) => moment(value).startOf('hour').fromNow();
Vue.filter('timeFromNow', timeFromNowFormatter);

// this allows to call this function from an js code; to learn more about that read about javascript modules
// import TimeFromNowFilter from 'src/TimeFromNowFilter.js'
//    TimeFromNowFilter(dateStrVAlue)
export default timeFromNowFormatter;
