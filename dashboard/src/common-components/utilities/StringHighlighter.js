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

const escHtml = (s) =>
    s.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;')

export default class StringHighlighter {
  static highlight(value, searchStr) {
    const searchStringNorm = searchStr.trim().toLowerCase();
    const index = value.toLowerCase().indexOf(searchStringNorm);
    if (index < 0) {
       return null;
    }
    const before = escHtml(value.substring(0, index))
    const match = escHtml(value.substring(index, index + searchStringNorm.length))
    const after = escHtml(value.substring(index + searchStringNorm.length))
    const htmlRel = `${before}<mark>${match}</mark>${after}`;
    return htmlRel;
  }
}
