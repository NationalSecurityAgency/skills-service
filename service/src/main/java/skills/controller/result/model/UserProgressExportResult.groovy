/**
 * Copyright 2024 SkillTree
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
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView

class UserProgressExportResult extends AbstractXlsxStreamingView {
    static final String PROJECT_ID = "projectId"
    static final String USERS_DATA = "projectUsers"
    static final String USER_TAG_LABEL = "userTagLabel"
    static final String HEADER_AND_FOOTER = "headerAndFooter"

    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // define excel file name to be exported
        String userTagLabel = model.get(USER_TAG_LABEL)
        String projectId = model.get(PROJECT_ID)
        String exportHeaderAndFooter = model.get(HEADER_AND_FOOTER)
        response.addHeader("Content-Disposition", "attachment;fileName=${projectId}-users-${new Date().format("yyyy-MM-dd")}.xlsx")
        Sheet sheet = workbook.createSheet()
        Integer rowNum = 0
        Integer columnNumber = 0
        if (exportHeaderAndFooter) {
            addDataHeader(workbook, sheet, rowNum++, userTagLabel ? 7 : 6, exportHeaderAndFooter)
        }
        Row headerRow = sheet.createRow(rowNum++)
        headerRow.createCell(columnNumber++).setCellValue("User ID")
        headerRow.createCell(columnNumber++).setCellValue("User Name")
        if (userTagLabel) {
            headerRow.createCell(columnNumber++).setCellValue(userTagLabel)
        }
        headerRow.createCell(columnNumber++).setCellValue("Level")
        headerRow.createCell(columnNumber++).setCellValue("Current Points")
        headerRow.createCell(columnNumber++).setCellValue("Points First Earned")
        headerRow.createCell(columnNumber++).setCellValue("Points Last Earned")

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 14) // NOTE: 14 = "mm/dd/yyyy"

        Cell cell = null
        model.get(USERS_DATA).each { ProjectUser user ->
            columnNumber = 0
            Row row = sheet.createRow(rowNum++)
            row.createCell(columnNumber++).setCellValue(user.userIdForDisplay)
            row.createCell(columnNumber++).setCellValue("${user.lastName ?: ''}${user.firstName && user.lastName ? ', ': ''}${user.firstName ?: ''}")
            if (userTagLabel) {
                row.createCell(columnNumber++).setCellValue(user.userTag)
            }
            row.createCell(columnNumber++).setCellValue(user.userMaxLevel)
            row.createCell(columnNumber++).setCellValue(user.totalPoints)

            cell = row.createCell(columnNumber++)
            cell.setCellStyle(dateStyle)
            cell.setCellValue(user.firstUpdated)
            cell = row.createCell(columnNumber++)
            cell.setCellStyle(dateStyle)
            cell.setCellValue(user.lastUpdated)
        }

        if (exportHeaderAndFooter) {
            addDataHeader(workbook, sheet, rowNum++, userTagLabel ? 7 : 6, exportHeaderAndFooter)
        }
    }

    private static void addDataHeader(Workbook workbook, Sheet sheet, Integer rowNum, Integer numCols, String exportHeaderAndFooter) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(exportHeaderAndFooter)
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numCols - 1))
    }
}
