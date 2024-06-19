/*
 * Copyright 2024 SkillTree
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
export const useLanguagePluralSupport = () => {
  const plural = (param) => {
    let res = ''
    if (param instanceof Array) {
      res = param && param.length > 1 ? 's' : ''
    }
    if (typeof param === 'number') {
      res=  param !== 1 ? 's' : ''
    }
    return res
  }

  const areOrIs = (numItems) => {
    return (numItems > 1) ? 'are' : 'is';
  }
  const sOrNone = (numItems) => {
    return (numItems > 1) ? 's' : '';
  }

  const pluralWithHave = (arr) => {
    return arr && arr.length > 1 ? 's have' : ' has'
  }

  return {
    plural,
    pluralWithHave,
    areOrIs,
    sOrNone
  }
}