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

// Prefer structured explanations; known HTTP statuses also have actionable messages.
const getUserMessage = (details) => [details?.explanation, details?.message]
    .find((message) => typeof message === 'string' && message.trim());

const statusMessages = {
    429: 'AI usage limit reached. Please try again later or contact your administrator.',
    503: 'The AI provider is temporarily unavailable. Please try again later.',
    504: 'The AI provider timed out. Please try again.',
};

export const useOpenaiService = () => {

    let currentRequestController = null;

    const prompt = async (promptParams, onChunk, onComplete, onError) => {
        try {
            currentRequestController = new AbortController();
            const response = await fetch(`/openai/chat`, {
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
                let userMessage = statusMessages[response.status];
                // Try to get error details from response if available
                try {
                    const errorData = await response.json();
                    userMessage = getUserMessage(errorData) || userMessage;
                    errorMessage = userMessage || errorMessage;
                } catch (e) {
                    // If we can't parse JSON, use the status text
                    errorMessage = response.statusText || errorMessage;
                }
                const error = new Error(`Failed to generate description: ${errorMessage}`);
                error.userMessage = userMessage;
                error.retryAfter = response.headers.get('Retry-After');
                throw error;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let pending = '';

            const processEvent = (event) => {
                const lines = event.split(/\r?\n/);
                const eventType = lines.find((line) => line.startsWith('event:'))?.substring(6).trim();
                const content = lines.filter((line) => line.startsWith('data:'))
                    .map((line) => line.substring(5)).join('\n');
                if (eventType === 'error') {
                    const details = JSON.parse(content);
                    const userMessage = getUserMessage(details);
                    const error = new Error(userMessage || 'AI generation failed. Please try again.');
                    error.userMessage = userMessage;
                    error.retryAfter = details.retryAfter;
                    throw error;
                }
                if (content === '[DONE]') return true;
                if (content) onChunk(content.replace(/<<newline>>/g, '\n'));
                return false;
            };

            try {
                while (true) {
                    const {done, value} = await reader.read();
                    pending += done ? decoder.decode() : decoder.decode(value, {stream: true});
                    let separator;
                    while ((separator = /\r?\n\r?\n/.exec(pending))) {
                        const event = pending.substring(0, separator.index);
                        pending = pending.substring(separator.index + separator[0].length);
                        if (processEvent(event)) {
                            currentRequestController = null;
                            if (onComplete) onComplete();
                            return;
                        }
                    }
                    if (done) {
                        if (pending.trim()) throw new Error('The AI response was interrupted. Please try again.');
                        currentRequestController = null;
                        if (onComplete) onComplete();
                        break;
                    }
                }
            } finally {
                await reader.cancel().catch(() => {
                });
                reader.releaseLock();
            }
        } catch (error) {
            currentRequestController = null;
            if (onError) {
                console.error(error)
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
