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
class GlobalQuizRunsExportResult extends AbstractXlsxStreamingView {

    static final String USER_QUERY = "userQuery"
    static final String NAME_QUERY = "nameQuery"
    static final String USER_ID_FILTER = "userIdFilter"
    static final String PAGE_REQUEST = "pageRequest"
    static final String START_DATE = "startDate"
    static final String END_DATE = "endDate"

    @Autowired
    private ExcelExportService excelExportService

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userQuery = model.get(USER_QUERY) as String
        String nameQuery = model.get(NAME_QUERY) as String
        String userIdFilter = model.get(USER_ID_FILTER) as String
        PageRequest pageRequest = model.get(PAGE_REQUEST) as PageRequest
        Date startDate = model.get(START_DATE) as Date
        Date endDate = model.get(END_DATE) as Date

        // define excel file name to be exported
        response.addHeader("Content-Disposition", "attachment;fileName=global-quiz-runs-${new Date().format("yyyy-MM-dd")}.xlsx")
        excelExportService.exportQuizRuns(workbook, userQuery, nameQuery, userIdFilter, pageRequest, startDate, endDate)
    }
}
