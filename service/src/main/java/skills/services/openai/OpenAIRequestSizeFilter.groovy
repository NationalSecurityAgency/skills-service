/**
 * Copyright 2026 SkillTree
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
package skills.services.openai

import groovy.json.JsonOutput
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/** Bounds allocation before Jackson reads the chat history, including requests without a length header. */
@Component
class OpenAIRequestSizeFilter extends OncePerRequestFilter {
    private final OpenAIUsageLimitsProperties limits

    OpenAIRequestSizeFilter(OpenAIUsageLimitsProperties limits) {
        this.limits = limits
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limits.maxRequestBytes == null || request.method != 'POST' ||
                request.servletPath != '/openai/chat'
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        int limit = limits.maxRequestBytes
        if (request.contentLengthLong > limit) {
            reject(response)
            return
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(limit, 8192))
        byte[] chunk = new byte[8192]
        int count
        while ((count = request.inputStream.read(chunk, 0, (int) Math.min(chunk.length as long, (long) limit - buffer.size() + 1))) != -1) {
            if ((long) buffer.size() + count > limit) {
                reject(response)
                return
            }
            buffer.write(chunk, 0, count)
        }
        chain.doFilter(new BufferedRequest(request, buffer.toByteArray()), response)
    }

    private static void reject(HttpServletResponse response) {
        response.status = 413
        response.contentType = 'application/json'
        response.writer.write(JsonOutput.toJson([success: false, errorCode: 'BadParam', explanation: 'AI request body exceeds configured byte limit']))
    }

    private static class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body

        BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request)
            this.body = body
        }

        @Override
        ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body)
            return new ServletInputStream() {
                @Override
                int read() { input.read() }

                @Override
                int read(byte[] bytes, int offset, int length) { input.read(bytes, offset, length) }

                @Override
                boolean isFinished() { input.available() == 0 }

                @Override
                boolean isReady() { true }

                @Override
                void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException('Chat request bodies use blocking servlet reads')
                }
            }
        }

        @Override
        BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(inputStream, characterEncoding ?: 'UTF-8'))
        }
    }
}
