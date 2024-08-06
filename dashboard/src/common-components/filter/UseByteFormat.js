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
export const useByteFormat = () => {
  const prettyBytes = (bytes) => {
    if (typeof bytes !== 'number' || Number.isNaN(bytes)) {
      throw new TypeError('Expected a number');
    }
    const term = 1024;
    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const neg = bytes < 0;
    let retVal = bytes;

    if (neg) {
      retVal = -retVal;
    }

    if (retVal < 1) {
      return `${(neg ? '-' : '') + retVal} B`;
    }

    const exponent = Math.min(Math.floor(Math.log(retVal) / Math.log(term)), units.length - 1);
    retVal = (retVal / (term ** exponent)).toFixed(2) * 1;
    const unit = units[exponent];

    return `${(neg ? '-' : '') + retVal} ${unit}`;
  };

  return {
    prettyBytes
  }
}