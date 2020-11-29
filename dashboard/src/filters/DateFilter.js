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

const dateFormatter = (value) => window.dayjs(value).format('YYYY-MM-DD HH:mm');
Vue.filter('date', dateFormatter);

// this allows to call this function from an js code; to learn more about that read about javascript modules
// import DateFilter from 'src/DateFilter.js'
//    DateFilter(dateStrVAlue)
export default dateFormatter;
