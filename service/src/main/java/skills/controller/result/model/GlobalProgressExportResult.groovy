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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView
import skills.services.ExcelExportService

@Component
class GlobalProgressExportResult extends AbstractXlsxStreamingView {
    static final String QUERY = "query"
    static final String USER_TAG_FILTER = "userTagFilter"
    static final String PAGE_REQUEST = "pageRequest"

    @Autowired
    private ExcelExportService excelExportService

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userQuery = model.get(QUERY) as String
        String userTagFilter = model.get(USER_TAG_FILTER) as String
        PageRequest pageRequest = model.get(PAGE_REQUEST) as PageRequest

        // define excel file name to be exported
        response.addHeader("Content-Disposition", "attachment;fileName=global-user-progress-${new Date().format("yyyy-MM-dd")}.xlsx")
        excelExportService.exportGlobalUserProgress(workbook, userQuery, pageRequest, userTagFilter)
    }
}
