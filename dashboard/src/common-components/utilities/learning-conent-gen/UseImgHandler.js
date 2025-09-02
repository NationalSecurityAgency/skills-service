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
import {useLog} from "@/components/utils/misc/useLog.js";


export const useImgHandler = () => {

    const log = useLog()

    /**
     * A unique, random string prefix for our placeholders to prevent collisions.
     * The use of a symbol in the prefix further reduces the risk of accidental matches.
     */
    const PLACEHOLDER_PREFIX = '---IMG_PLACEHOLDER-';

    /**
     * A regular expression to match standard Markdown image syntax: `![alt text](data:image)` with base64 encoded image.
     * The 'g' flag is for a global search to find all image matches.
     */
    const IMAGE_REGEX = /!\[([^\]]*?)\]\((data:image\/[^;]+;base64,[^)]+)\)/g;

    const extractImages = (markdown) => {
        const extractedImages = [];
        let processedText = markdown;
        let match;
        let imageIndex = 0;

        while ((match = IMAGE_REGEX.exec(processedText)) !== null) {
            const originalImageMarkdown = match[0];
            const altText = match[1];
            const imageUrl = match[2];
            const placeholder = `${PLACEHOLDER_PREFIX}${imageIndex}`;

            extractedImages.push({
                placeholder: placeholder,
                markdown: originalImageMarkdown,
                alt: altText,
                url: imageUrl
            });

            // Replace the image with the unique placeholder.
            // The match.index ensures we only replace the specific image instance.
            processedText = processedText.substring(0, match.index) +
                placeholder +
                processedText.substring(match.index + originalImageMarkdown.length);
            imageIndex++;
            IMAGE_REGEX.lastIndex = match.index + placeholder.length;
        }

        // Reset regex.lastIndex for future use
        IMAGE_REGEX.lastIndex = 0;

        return {
            processedText,
            extractedImages,
            numImagesExtracted: imageIndex,
            hasImages: imageIndex > 0
        };
    }

    const reinsertImages = (processedText, extractedImages) => {
        let restoredText = processedText;

        if (extractedImages) {
            // Process in reverse to handle multiple occurrences correctly
            [...extractedImages].reverse().forEach(image => {
                const placeholderRegex = new RegExp(image.placeholder, 'gi');
                restoredText = restoredText.replace(placeholderRegex, image.markdown);
            });
        }
        return restoredText;
    }

    const instructionsToKeepPlaceholders = () => {
        return ` In the provided text Images are represented with placeholders that follow the format of ${PLACEHOLDER_PREFIX}<number>, must keep these placeholders in the final text in the same spot.`
    }

    return {
        extractImages,
        reinsertImages,
        instructionsToKeepPlaceholders
    }

}