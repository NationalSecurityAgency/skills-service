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
export const usePdfBuilderHelper = () => {
  const arrowColor1 = '#264653'
  const arrowColor2 = '#2a9d8f'
  const arrowColor3 = '#e9c369'
  const arrowColor5 = '#e76f51'
  const lightGray = '#e1e1e1'
  const darkGreen = '#097151'
  const darkGray = '#504e4e'
  const successColor = '#097151'

  const resetTextStyle = (doc) => {
    doc.fillColor(arrowColor1)
    doc.fontSize(12)
  }


  const addHeader = (doc, overallTranscript, info) => {
    doAddHeaderOrFooter(doc, overallTranscript, info, 30, 'Header')
  }
  const addFooter = (doc, overallTranscript, info) => {
    doAddHeaderOrFooter(doc, overallTranscript, info, 760, 'Footer')
  }
  const doAddHeaderOrFooter = (doc, overallTranscript, info, y, title) => {
    if (info.headerAndFooter) {
      const headerSect = doc.struct('Sect', { title })
      overallTranscript.add(headerSect)

      headerSect.add(
        doc.struct('Div', () => {
          doc.fillColor(arrowColor5)
          doc.text(`${info.headerAndFooter} `, 50, y, {
            width: 500,
            align: 'center'
          })
        })
      )
      resetTextStyle(doc)
    }
  }
  const addTitle = (doc, section, title, y = null) => {
    doc.moveDown(1)
    section.add(
      doc.struct('H', () => {
        doc.fontSize(15).fillColor(darkGreen).text(`${title.toUpperCase()} `, 50, y)
      })
    )
    resetTextStyle(doc)
  }

  return {
    arrowColor1,
    arrowColor2,
    arrowColor3,
    arrowColor5,
    resetTextStyle,
    addHeader,
    addFooter,
    addTitle,
    lightGray,
    darkGreen,
    darkGray,
    successColor
  }
}