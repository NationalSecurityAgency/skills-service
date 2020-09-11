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

const truncateFormatter = (strValue, truncateTo = 30) => {
  const regexStr = `^.{0,${truncateTo}}[\\S]*`;
  const regex1 = new RegExp(regexStr, 'gi');

  let re = strValue.match(regex1);
  const l = re[0].length;
  re = re[0].replace(/\s$/, '');
  if (l < strValue.length) {
    re = `${re}...`;
  }

  const regexStr2 = `(.{${truncateTo}})..+`;
  const regex2 = new RegExp(regexStr2, 'gi');
  re = re.replace(regex2, '$1...');
  return re;
};
Vue.filter('truncate', truncateFormatter);

// this allows to call this function from an js code; to learn more about that read about javascript modules
// import NumberFilter from 'src/NumberFilter.js'
//    NumberFilter(myNumber)
export default truncateFormatter;
