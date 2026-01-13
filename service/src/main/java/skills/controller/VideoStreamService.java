/**
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

package skills.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import skills.controller.exceptions.SkillException;
import skills.storage.model.Attachment;
import skills.storage.repos.AttachmentRepo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class VideoStreamService {

    private static final int BUFFER_SIZE = 1024 * 8; // 8KB buffer
    private static final Logger log = LoggerFactory.getLogger(VideoStreamService.class);
    @Autowired
    AttachmentRepo attachmentRepo;

    @Value("${skills.config.videoStreamDefaultChunkSize:5242880}")
    private Long videoStreamDefaultChunkSize = (long) (5 * 1024 * 1024); // 5MB chunks (5242880)

    @Value("${skills.config.videoStreamMaxOptimizedDbFetchSize:10485760}")
    private Long videoStreamMaxOptimizedDbFetchSize = (long) (20 * 1024 * 1024); // 20MB chunks (5242880)

    @Value("${skills.config.suppressBrokenPipeException:true}")
    private Boolean suppressBrokenPipeException = true;

    @PostConstruct
    protected void init() {
        if (videoStreamDefaultChunkSize > videoStreamMaxOptimizedDbFetchSize) {
            throw new IllegalStateException(String.format(
                    "videoStreamDefaultChunkSize (%d) must be less than or equal to videoStreamMaxOptimizedDbFetchSize (%d)",
                    videoStreamDefaultChunkSize,
                    videoStreamMaxOptimizedDbFetchSize
            ));
        }
    }

    @Transactional(readOnly = true)
    public void streamVideo(
            Attachment attachment,
            String rangeHeader,
            HttpServletResponse response) {
        try {
            // 1. Set common response headers
            response.setContentType(attachment.getContentType());
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline; filename=\"" + attachment.getFilename() + "\"");

            long fileSize = attachment.getSize();
            long start = 0;
            long end = fileSize - 1;

            // 2. Handle range requests
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Parse the range header
                String rangeValue = rangeHeader.substring(6).trim();
                String[] ranges = rangeValue.split("-");

                try {
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1) {
                        end = Long.parseLong(ranges[1]);
                    } else {
                        // If end is not specified, use a reasonable default chunk
                        end = start + videoStreamDefaultChunkSize - 1;
                    }
                    // Ensure we don't go past the end of the file
                    if (end >= fileSize) {
                        end = fileSize - 1;
                    }

                    // Set partial content status and headers
                    response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                    response.setHeader("Content-Range",
                            String.format("bytes %d-%d/%d", start, end, fileSize));

                } catch (NumberFormatException e) {
                    // If range is invalid, just return the full content
                    start = 0;
                    end = fileSize - 1;
                }
            }

            long contentLength = end - start + 1;
            response.setContentLengthLong(contentLength);

            // 3A. if chunks are small enough load them into memory only selectin the bytes for this chunk
            if (contentLength <= videoStreamMaxOptimizedDbFetchSize) {
                byte[] chunk = attachmentRepo.fetchFileChunk(attachment.getUuid(), start, contentLength);
                try (OutputStream outputStream = response.getOutputStream()) {
                    outputStream.write(chunk, 0, chunk.length);
                } catch (AsyncRequestNotUsableException e) {
                    if (suppressBrokenPipeException && StringUtils.isNotBlank(e.getMessage())
                            && (e.getMessage().contains("Broken pipe") || e.getMessage().contains("Connection reset by peer"))
                    ){
                        log.debug("Client aborted the connection. This is typical of Video.js lib", e);
                    } else {
                        throw e;
                    }
                }
            } else {
                // 3B. Stream larger the content
                try (InputStream inputStream = attachment.getContent().getBinaryStream();
                     OutputStream outputStream = response.getOutputStream()) {

                    // Skip to the start position if needed
                    if (start > 0) {
                        long skipped = inputStream.skip(start);
                        if (skipped != start) {
                            throw new IOException("Failed to skip to the requested position for video with uuid=[" + attachment.getUuid() + "]");
                        }
                    }

                    // Stream the content in chunks
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long remaining = contentLength;
                    int read;

                    while ((read = inputStream.read(buffer, 0,
                            (int) Math.min(buffer.length, remaining))) != -1 && remaining > 0) {
                        outputStream.write(buffer, 0, read);
                        remaining -= read;
                        outputStream.flush();
                    }
                } catch (Exception e) {
                    if (!response.isCommitted()) {
                        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Error streaming video with uuid=[\" + attachment.getUuid() + \"]\": " + e.getMessage());
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new SkillException("Failed to stream video [" + attachment.getUuid() + "]", e);
        }
    }
}