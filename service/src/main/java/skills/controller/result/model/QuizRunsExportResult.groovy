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
class QuizRunsExportResult extends AbstractXlsxStreamingView {

    static final String QUIZ_IDS = "quizIds"
    static final String USER_QUERY = "userQuery"
    static final String NAME_QUERY = "nameQuery"
    static final String PAGE_REQUEST = "pageRequest"
    static final String START_DATE = "startDate"
    static final String END_DATE = "endDate"
    static final String IS_GLOBAL = "isGlobal"

    @Autowired
    private ExcelExportService excelExportService

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> quizIds = model.get(QUIZ_IDS) as List<String>
        String userQuery = model.get(USER_QUERY) as String
        String nameQuery = model.get(NAME_QUERY) as String
        String userIdFilter = ''
        PageRequest pageRequest = model.get(PAGE_REQUEST) as PageRequest
        Date startDate = model.get(START_DATE) as Date
        Date endDate = model.get(END_DATE) as Date
        boolean isGlobal = model.get(IS_GLOBAL) as boolean

        // define excel file name to be exported
        String exportType = quizIds.size() == 1 && !isGlobal ? "${quizIds[0]}" : "global"
        response.addHeader("Content-Disposition", "attachment;fileName=${exportType}-quiz-runs-${new Date().format("yyyy-MM-dd")}.xlsx")
        excelExportService.exportQuizRuns(workbook, quizIds, userQuery, nameQuery, userIdFilter, pageRequest, startDate, endDate)
    }
}
