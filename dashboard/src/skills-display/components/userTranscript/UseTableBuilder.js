/*
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
import { usePdfBuilderHelper } from '@/skills-display/components/userTranscript/UsePdfBuilderHelper.js'

export const useTableBuilder = () => {
  const lineGap = 5
  const lightGray = '#f5f9ff'
  const darkBlue = '#295bac'
  const arrowColor1 = '#264653'
  const tableStartY = 50

  const pdfHelper = usePdfBuilderHelper()

  const addTable = (doc, overallTranscript, sectionToAddTo, tableInfo) => {
    let remainingRows = doAddTable(doc, sectionToAddTo, tableInfo)
    if (!remainingRows) {
      sectionToAddTo.end()
    }
    while(remainingRows && remainingRows.length > 0) {
      sectionToAddTo.end()
      pdfHelper.addFooter(doc, overallTranscript, tableInfo)
      doc.addPage({ margin: 10 })
      doc.pageNum++
      const subjectStruct = doc.struct('Sect', { title: `${tableInfo.structTitle} (continued) ` })
      overallTranscript.add(subjectStruct)
      pdfHelper.addHeader(doc, overallTranscript, tableInfo)
      pdfHelper.addTitle(doc, subjectStruct, `${tableInfo.sectionTitle} (continued) `, tableStartY)
      const tableInfoCopy = {...tableInfo, rows: remainingRows }
      remainingRows = doAddTable(doc, subjectStruct, tableInfoCopy)
      subjectStruct.end()
    }
  }

  const doAddTable = (doc, sectionToAddTo, tableInfo) => {
    const startX = 50
    const totalWidth = doc.page.width - (startX*2)
    // allocate fixed with for 2-N column, first column would take the res
    doc.lineGap(lineGap)

    const additionalWidthForSmallColumns = 30
    const columnWidths =tableInfo.headers.map((header, index) => {
      const cellsInColumnWidths = tableInfo.rows.map((row) => Math.trunc(doc.widthOfString(row[index])))
      const maxCellInColumnWidth = Math.max(...cellsInColumnWidths)
      return maxCellInColumnWidth + additionalWidthForSmallColumns
    })
    // adjust the first column to be the remaining of the rest
    columnWidths[0] = totalWidth - columnWidths.slice(1).reduce((a, b) => a + b, 0)
    let currentColumnX = startX
    const headersWrapped = tableInfo.headers.map((header, index) => {
      if (index > 0) {
        currentColumnX += columnWidths[index - 1]
      }
      return {
        value: header, x: currentColumnX, width: columnWidths[index]
      }
    })
    const rowsWrapped = tableInfo.rows.map((row) => {
      return row.map((cell, index) => {
        const column = tableInfo.headers[index]
        return {
          value: cell, column, x: headersWrapped[index].x
        }
      })
    })

    const table = doc.struct('Table', { title: tableInfo.structTitle })
    sectionToAddTo.add(table)

    const addTableHeaders = () => {
      const headerRowConstruct = doc.struct('TR', { title: `Header Row` })
      table.add(headerRowConstruct)
      addRectangle(doc, headerRowConstruct, true)
      headersWrapped.forEach((cell, cellIndex) => {
        if (cellIndex > 0) {
          doc.moveUp()
        }
        const cellStruct = doc.struct('TH', { title: `${cell.column} Column Header` }, () => {
          doc.text(`${cell.value} `, cell.x)
        })
        headerRowConstruct.add(cellStruct)
      })

      doc.fillColor(arrowColor1)
    }
    addTableHeaders()

    for (let rowIndex = 0; rowIndex < rowsWrapped.length; rowIndex++) {
      const row = rowsWrapped[rowIndex]
      const rowStruct = doc.struct('TR', { title: `Row ${rowIndex}` })
      table.add(rowStruct)
      if (rowIndex === 0 || rowIndex % 2 === 0) {
        const longCellValue = row.reduce((longest, current) => {
          return current.value.length > longest.length ? current.value : longest;
        }, '');
        const widthOfLongestCell = doc.widthOfString(longCellValue)
        const maxWidth = headersWrapped[0].width
        const numRows = Math.trunc(widthOfLongestCell / maxWidth) + 1
        addRectangle(doc, rowStruct, false, numRows)
      }

      row.forEach((cell, cellIndex) => {
        const myHeading = headersWrapped[cellIndex]
        if (cellIndex > 0) {
          doc.moveUp()
        }
        const cellStruct = doc.struct('TD', { title: `Row ${rowIndex} ${cell.column} Column` }, () => {
          doc.text(`${cell.value} `, cell.x, null, { width: myHeading.width })
        })
        rowStruct.add(cellStruct)
      })
      if (doc.y > 700 && rowIndex < (tableInfo.rows.length - 2)) {
        return tableInfo.rows.splice(rowIndex +1, tableInfo.rows.length - rowIndex)
      }
    }

    return null
  }

  const addRectangle = (doc, parentSection, isHeader = false, numRows = 1) => {
    const rowBackgroundStruct = doc.struct('Artifact', { type: 'Layout' }, () => {
      doc.fillColor(isHeader ? darkBlue : lightGray)
      const lineHeight = doc.currentLineHeight() + lineGap

      const startX = 47
      const totalWidth = doc.page.width - (startX * 2)

      const y = doc.y
      doc.rect(startX, y - lineGap, totalWidth, (lineHeight + 2) * numRows)
      doc.fill()
      doc.fillColor(isHeader ? 'white' : arrowColor1)
    })
    parentSection.add(rowBackgroundStruct)
  }

  return {
    addTable
  }
}