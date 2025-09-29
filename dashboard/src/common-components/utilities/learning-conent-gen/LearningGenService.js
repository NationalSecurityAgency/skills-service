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
import axios from 'axios';

export default {
    generateDescription(projectId, instructions) {
        return axios.post(`/admin/projects/${encodeURIComponent(projectId)}/generateDescription`, { instructions}, { handleError: false })
            .then((response) => response.data);
    },
    async generateDescriptionStreamWithFetch(projectId, instructions, onChunk, onComplete, onError) {
        try {
            const response = await fetch(`/openai/stream/description`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify({ instructions })
            })

            // Check if the response is not OK (status code 200-299)
            if (!response.ok) {
                let errorMessage = `Server responded with status ${response.status}`;
                // Try to get error details from response if available
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                } catch (e) {
                    // If we can't parse JSON, use the status text
                    errorMessage = response.statusText || errorMessage;
                }
                throw new Error(`Failed to generate description: ${errorMessage}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                console.log('reading')
                const {done, value} = await reader.read();
                console.log('done reading')
                if (done) {
                    if (onComplete) onComplete();
                    break;
                }

                const receivedValue = decoder.decode(value, {stream: true});
                console.log(`receivedValue: ${receivedValue}`)
                const lines = receivedValue.split(/\n/);

                const processedLines = []
                for (const line of lines) {
                    const content = line.startsWith('data:') ? line.substring(5) : line
                    if (content === '[DONE]') {
                        if (onComplete) onComplete();
                        return;
                    }
                    if (content) {
                        const toPush = content.replace(/<<newline>>/g, '\n');
                        processedLines.push(toPush)
                    }
                }

                const processed = processedLines.join('')
                console.log(`processed: ${processed} from lines: ${JSON.stringify(lines)}`)
                onChunk(processed)
            }
        } catch (error) {
            if (onError) {
                onError(error);
            } else {
                throw error; // Re-throw if no error handler is provided
            }
        }
    }
};