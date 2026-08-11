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
package skills.controller.result.model

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.web.servlet.view.AbstractView

/**
 * Base class for streaming XLSX export views, replacing the Spring-provided
 * {@code AbstractXlsxStreamingView} which was deprecated in Spring Framework 7.0
 * for removal. Replicates identical behavior: creates an {@link SXSSFWorkbook},
 * delegates to {@link #buildExcelDocument}, writes to the response output stream,
 * and disposes temporary POI streaming files.
 */
abstract class AbstractSkillsXlsxStreamingView extends AbstractView {

    AbstractSkillsXlsxStreamingView() {
        setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true
    }

    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SXSSFWorkbook workbook = new SXSSFWorkbook()
        try {
            buildExcelDocument(model, workbook, request, response)
            response.setContentType(getContentType())
            workbook.write(response.getOutputStream())
            response.flushBuffer()
        } finally {
            workbook.dispose()
        }
    }

    abstract protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception
}
