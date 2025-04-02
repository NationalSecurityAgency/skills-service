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
export const useColors = () => {
  const colors = ['text-blue-500', 'text-green-500', 'text-cyan-500', 'text-indigo-500', 'text-teal-500', 'text-orange-500', 'text-purple-500']
  const bgColors = ['bg-blue', 'bg-green', 'bg-cyan', 'bg-indigo', 'bg-teal', 'bg-orange', 'bg-purple']

  const getFromArrBasedOnIndex = (arr, index) => {
    const colorIndex = index % arr.length
    return arr[colorIndex]
  }

  const getTextClass = (index) => {
    return getFromArrBasedOnIndex(colors, index)
  }

  const getBgClass = (index, bgWeightNumber) => {
    return `${getFromArrBasedOnIndex(bgColors, index)}-${bgWeightNumber}`
  }

  const goldText = 'text-yellow-600'
  const silverText = 'text-slate-600'
  const bronzeText = 'text-orange-800'

  const getRankTextClass = (rank) => {
    if (rank === 1) {
      return goldText;
    }
    if (rank === 2) {
      return silverText;
    }
    if (rank === 3) {
      return bronzeText;
    }
    return null;
  }

  return {
    getTextClass,
    getRankTextClass,
    getBgClass
  }
}