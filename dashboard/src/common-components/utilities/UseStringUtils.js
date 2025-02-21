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
export const useStringUtils = () => {

    const addNewlinesToChunks = (chunks, truncateTo = 25) => {
        let processedString = '';
        let currentSize = 0;
        for (let chunk of chunks) {
            if (currentSize >= truncateTo) {
                processedString += '\n';
                currentSize = 0;
            }
            processedString += chunk + ' ';
            currentSize += chunk.length
        }
        return processedString
    }

    const addNewlinesToString = (strValue, truncateTo = 25) => {
        let numberOfSegments = strValue.length / truncateTo;
        let processedString = strValue
        for(let x = 1; x < numberOfSegments; x++) {
            processedString = processedString.slice(0, x * truncateTo) + '-\n' + processedString.slice(x * truncateTo)
        }
        return processedString
    }

    return {
        addNewlinesToString,
        addNewlinesToChunks
    }
}