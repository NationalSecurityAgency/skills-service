/*
 * Copyright 2025 SkillTree
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
export const useChartSupportColors = () => {

    const getColors = () => {
        const documentStyle = getComputedStyle(document.documentElement);
        const textColor = documentStyle.getPropertyValue('--p-text-color');
        const textMutedColor = documentStyle.getPropertyValue('--p-text-muted-color');
        const contentBorderColor = documentStyle.getPropertyValue('--p-content-border-color');
        const surface100Color = documentStyle.getPropertyValue('--p-surface-100')
        const surface600Color = documentStyle.getPropertyValue('--p-surface-400')
        const green700Color = documentStyle.getPropertyValue('--p-green-700')

        return {
            textColor,
            textMutedColor,
            contentBorderColor,
            surface100Color,
            surface600Color,
            green700Color,
        }
    }

    const borderColorBank  = [
        'rgba(249, 115, 22)', // bg-orange-500
        'rgba(6, 182, 212)', // bg-cyan-400
        'rgba(107, 114, 128)', // bg-gray-500
        'rgba(139, 92, 246)', // bg-violet-500
        'rgba(16, 185, 129)',  // bg-emerald-500
        'rgba(239, 68, 68)',    // bg-red-500
        'rgba(234, 179, 8)',    // bg-yellow-500
        'rgba(34, 197, 94)',    // bg-green-500
        'rgba(59, 130, 246)',   // bg-blue-500
        'rgba(168, 85, 247)',   // bg-purple-500
        'rgba(236, 72, 153)',   // bg-pink-500
        'rgba(20, 184, 166)',   // bg-teal-500
    ]
    const backgroundColorBank = borderColorBank.map(color =>
        color.endsWith(')') ? color.slice(0, -1) + ', 0.8)' : color + ', 0.8)'
    );
    const getBackgroundColorArray = (numColors) => {
        return Array.from({ length: numColors }, (_, i) =>
            backgroundColorBank[i % backgroundColorBank.length]
        )
    }
    const getBorderColorArray = (numColors) => {
        return Array.from({ length: numColors }, (_, i) =>
            borderColorBank[i % backgroundColorBank.length]
        )
    }

    const getSolidColor = (arrIndex) => {
        return borderColorBank[arrIndex % borderColorBank.length]
    }
    const getTranslucentColor = (arrIndex, opacity = 0.8) => {
        const color = getSolidColor(arrIndex)
        return color.endsWith(')') ? color.slice(0, -1) + `, ${opacity})` : color + `, ${opacity})`
    }

    return {
        getColors,
        getBackgroundColorArray,
        getBorderColorArray,
        getSolidColor,
        getTranslucentColor
    }
}