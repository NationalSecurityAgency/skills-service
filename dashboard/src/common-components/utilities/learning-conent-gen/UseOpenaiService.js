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
import axios from "axios";

export const useOpenaiService = () => {

    let currentRequestController = null;

    const prompt = async (promptParams, onChunk, onComplete, onError) => {
        try {
            currentRequestController = new AbortController();
            const response = await fetch(`/openai/stream/description`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify(promptParams),
                signal: currentRequestController.signal  // Add the signal to the request
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
                const {done, value} = await reader.read();
                if (done) {
                    currentRequestController = null;
                    if (onComplete) onComplete();
                    break;
                }

                const receivedValue = decoder.decode(value, {stream: true});
                const lines = receivedValue.split(/\n/);

                const processedLines = []
                for (const line of lines) {
                    const content = line.startsWith('data:') ? line.substring(5) : line
                    if (content === '[DONE]') {
                        currentRequestController = null;
                        if (processedLines.length > 0) {
                            onChunk(processedLines.join(''))
                        }
                        if (onComplete) onComplete();
                        return;
                    }
                    if (content) {
                        const toPush = content.replace(/<<newline>>/g, '\n');
                        processedLines.push(toPush)
                    }
                }

                onChunk(processedLines.join(''))
            }
        } catch (error) {
            currentRequestController = null;
            if (onError) {
                onError(error);
            } else {
                throw error; // Re-throw if no error handler is provided
            }
        }
    }

    const cancelCurrentPrompt = () => {
        if (currentRequestController) {
            currentRequestController.abort();
            currentRequestController = null;
        }
    }

    const getAvailableModels = () => {
        return axios.get(`/openai/models`, {handleError: false})
            .then((response) => response.data);
    }

    return {
        prompt,
        cancelCurrentPrompt,
        getAvailableModels
    }
}
