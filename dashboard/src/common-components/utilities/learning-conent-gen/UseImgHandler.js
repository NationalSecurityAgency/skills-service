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
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import {useCommonMarkdownOptions} from '@/common-components/utilities/markdown/UseCommonMarkdownOptions.js';


export const useImgHandler = () => {

    const log = useLog()

    /**
     * A unique, random string prefix for our placeholders to prevent collisions.
     * The use of a symbol in the prefix further reduces the risk of accidental matches.
     */
    const PLACEHOLDER_PREFIX = '---IMG-';

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
        if (!extractedImages?.length) {
            return { text: processedText, unusedImages: [] };
        }

        const imagesByPlaceholder = new Map(extractedImages.map((image) => [image.placeholder, image]));
        // Match once so placeholders in the original image's alt text are not expanded.
        // AI responses may Markdown-escape any of the placeholder's hyphens.
        const placeholders = extractedImages.map((image) => escapeRegExp(image.placeholder).replace(/-/g, '\\\\?-'));
        const placeholderRegex = new RegExp(`(?:${placeholders.join('|')})(?!\\d)`, 'g');
        const usedImages = new Set();
        const probeUrls = [];
        const probeId = Array.from(crypto.getRandomValues(new Uint32Array(4)), (value) => value.toString(16).padStart(8, '0')).join('');
        const probeText = processedText.replace(placeholderRegex, (placeholder) => {
            const image = imagesByPlaceholder.get(placeholder.replace(/\\-/g, '-'));
            // Only base64 image URLs are eligible for restoration. Keeping their syntax
            // equivalent to the probes prevents the real data from changing parsing.
            if (!/^data:image\/[a-z\d.+-]+;base64,[a-z\d+/=]+$/i.test(image.url)) {
                throw unsafeImagePlacementError();
            }
            const probeUrl = `${image.url.substring(0, image.url.indexOf(',') + 1)}${probeId}${probeUrls.length}X`;
            probeUrls.push(probeUrl);
            usedImages.add(image);
            return `![${image.alt}](${probeUrl})`;
        });

        if (probeUrls.length) {
            validateImagePlacement(probeText, probeUrls);
        }

        return {
            // Use a callback so dollar signs in alt text remain literal replacement text.
            text: processedText.replace(placeholderRegex, (placeholder) => imagesByPlaceholder.get(placeholder.replace(/\\-/g, '-')).markdown),
            unusedImages: extractedImages.filter((image) => !usedImages.has(image))
        };
    }

    const unsafeImagePlacementError = () => {
        const error = new Error('Generated content placed an image placeholder in an unsafe context.');
        error.userMessage = 'The AI placed an image placeholder in an invalid location. Please ask it to regenerate with image placeholders only where images should be displayed, not inside links, HTML attributes, or code.';
        return error;
    }

    const validateImagePlacement = (markdown, probeUrls) => {
        const renderedHtml = [];
        // Use the same renderer as the preview, but NEVER attach its generated HTML
        // to a live element. Even detached image elements can initiate requests.
        const viewer = new Viewer({
            ...useCommonMarkdownOptions().markdownOptions,
            el: document.createElement('div'),
            initialValue: markdown,
            usageStatistics: false,
            customHTMLSanitizer: (html) => {
                renderedHtml.push(html);
                return '';
            }
        });
        try {
            const html = renderedHtml.join('');
            // Template contents are inert, including images and other remote resources.
            const template = document.createElement('template');
            template.innerHTML = html;
            const imageSources = [...template.content.querySelectorAll('img')].map((image) => image.getAttribute('src'));
            for (const probeUrl of probeUrls) {
                // Each occurrence must become exactly one complete image src, and
                // must not also appear in a link, attribute, raw HTML, or code sample.
                if (html.split(probeUrl).length !== 2 || imageSources.filter((src) => src === probeUrl).length !== 1) {
                    throw unsafeImagePlacementError();
                }
            }
        } finally {
            viewer.destroy();
        }
    }
    // Helper function to escape special regex characters
    function escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
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
