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
import format from 'number-format.js';

const numberFormatter = (value, fractionSize) => {
  let formatString = '#,##0.';
  if (fractionSize || fractionSize === 0) {
    formatString = `${formatString}${'0'.repeat(fractionSize)}`;
  }
  const fv = parseFloat(value);
  return format(formatString, fv);
};
Vue.filter('number', numberFormatter);

// this allows to call this function from an js code; to learn more about that read about javascript modules
// import NumberFilter from 'src/NumberFilter.js'
//    NumberFilter(myNumber)
export default numberFormatter;
