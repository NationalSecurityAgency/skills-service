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
import PDFDocument from 'pdfkit/js/pdfkit.standalone.js'
import { useBase64Images } from '@/skills-display/components/userTranscript/UseBase64Images.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import dayjs from '@/common-components/DayJsCustomizer.js'
import { useTableBuilder } from '@/skills-display/components/userTranscript/UseTableBuilder.js'

export const useTranscriptPdfExport1 = () => {

  const base64Images = useBase64Images()
  const numFormat = useNumberFormat()
  const timeUtils = useTimeUtils()
  const tableBuilder = useTableBuilder()

  const arrowColor1 = '#264653'
  const arrowColor2 = '#2a9d8f'
  const arrowColor3 = '#e9c369'
  const arrowColor5 = '#e76f51'
  const lightGray = '#e1e1e1'
  const darkGreen = '#097151'
  const darkGray = '#504e4e'
  const successColor = '#097151'
  const margin = 50
  const lineGap = 3
  const numSubjectWhenEarnedBadgesOnNewPage = 16


  const resetTextStyle = (doc) => {
    doc.fillColor(arrowColor1)
    doc.fontSize(12)
  }

  const buildNumOutOfOtherNum = (num, otherNum, addAchievedWord = true) => {
    let res = `${numFormat.pretty(num)} / ${numFormat.pretty(otherNum)}`
    if (addAchievedWord && num === otherNum && otherNum > 0) {
      res += ` (Achieved)`
    }
    return res
  }

  const generatePdf = (info) => {
    const doc = new PDFDocument({
      pdfVersion: '1.5',
      lang: 'en-US',
      tagged: true,
      displayTitle: true,
      bufferPages: true
    })
    doc.pageNum = 1
    doc.lineGap(lineGap)
    setDocMetaData(doc, info)

    // Document
    const overallTranscript = doc.struct('Document')
    doc.addStructure(overallTranscript)

    // Transcript -> header
    addHeader(doc, overallTranscript, info)

    // Transcript -> subject pages
    addSubjectPagesOfSkills(doc, overallTranscript, info)

    // Transcript -> subjects progress
    doc.switchToPage(0)
    // Transcript -> stats
    addOverallStats(doc, overallTranscript, info, info.labelsConf)
    addSubjectsProgress(doc, overallTranscript, info)

    // Transcript -> earned badge
    addEarnBadges(doc, overallTranscript, info)

    download(doc, info)
  }

  const addSubjectsProgress = (doc, overallTranscript, info) => {
    if (info.subjects && info.subjects.length > 0) {
      const subjectsProgress = doc.struct('Sect', { title: `Subjects Progress` })
      overallTranscript.add(subjectsProgress)
      addTitle(doc, subjectsProgress, 'Subjects Progress')

      const tableInfo = {
        title: 'Subjects Progress Table',
        headers: [`${info.labelsConf.subject}`, `${info.labelsConf.level}`, 'Points', `${info.labelsConf.skill}s`],
        rows: info.subjects.map((subject) => {
          return [
            (subject.pageNum >= 0) ? { value: subject.name, link: subject.pageNum } : subject.name,
            buildNumOutOfOtherNum(subject.userLevel, subject.totalLevels, false),
            buildNumOutOfOtherNum(subject.userPoints, subject.totalPoints, false),
            buildNumOutOfOtherNum(subject.userSkillsCompleted, subject.totalSkills, false)
          ]
        })
      }
      tableBuilder.addTable(doc, subjectsProgress, tableInfo)

      subjectsProgress.end()
    }
  }

  const addEarnBadges = (doc, overallTranscript, info) => {
    if (info.achievedBadges) {
      if (info.subjects.length >= numSubjectWhenEarnedBadgesOnNewPage) {
        doc.addPage()
        doc.pageNum++
      }
      const earnedBadgesStruct = doc.struct('Sect', { title: `Earned Badges` })
      overallTranscript.add(earnedBadgesStruct)
      addTitle(doc, earnedBadgesStruct, 'Earned Badges')

      const tableInfo = {
        title: 'Earned Badges Table',
        headers: [`${info.labelsConf.badge}`, 'Achieved On'],
        rows: info.achievedBadges.map((badge) => {
          return [
            badge.name,
            timeUtils.formatDate(dayjs(badge.dateAchieved))
          ]
        })
      }
      tableBuilder.addTable(doc, earnedBadgesStruct, tableInfo)

      earnedBadgesStruct.end()
    }
  }

  const addSubjectPagesOfSkills = (doc, overallTranscript, info) => {
    info.subjects.forEach((subject) => {
      if (subject.skills && subject.skills.length > 0) {
        doc.addPage()
        subject.pageNum = doc.pageNum
        const subjectStruct = doc.struct('Sect', { title: `${subject.name} Subject` })
        overallTranscript.add(subjectStruct)
        doc.pageNum++

        addTitle(doc, subjectStruct, `Subject: ${subject.name}`)

        addOverallStats(doc, subjectStruct, subject, info.labelsConf, false)

        const tableInfo = {
          title: `Skill Progress Table for ${subject.name} subject`,
          headers: [`${info.labelsConf.skill}`, 'Points'],
          rows: subject.skills.map((skill) => {
            return [
              skill.name,
              buildNumOutOfOtherNum(skill.userPoints, skill.totalPoints)
            ]
          })
        }
        doc.moveDown(0.5)
        tableBuilder.addTable(doc, subjectStruct, tableInfo)

        subjectStruct.end()
      }
    })
  }

  const setDocMetaData = (doc, info) => {
    // Set some meta data
    doc.info['Title'] = `SkillTree ${info.userName} TRANSCRIPT for ${info.userName}`
    doc.info['Author'] = 'SkillTree'
    doc.info['Subject'] = 'Project Transcript'
    doc.info['Keywords'] = 'skilltree,transcript,pdf,project'
  }

  const addHeader = (doc, overallTranscript, info) => {
    const titlePageHeader = doc.struct('Sect')
    overallTranscript.add(titlePageHeader)

    titlePageHeader.add(
      doc.struct('Span', { alt: 'SkillTree Logo Arrows Image' }, () => {
        doc.image(base64Images.logoArrows, 42, 73, { fit: [25, 60] })
      })
    )
    titlePageHeader.add(
      doc.struct('H', () => {
        doc.fontSize(20).fillColor(darkGreen).text('SkillTree TRANSCRIPT')
      })
    )
    resetTextStyle(doc)

    titlePageHeader.add(
      doc.struct('Span', { alt: 'SkillTree Logo Image' }, () => {
        doc.image(base64Images.logo, 470, 68, { fit: [90, 50] })
      })
    )

    titlePageHeader.add(
      doc.struct('P', () => {
        doc.fontSize(17).text(info.projectName)
        doc.fontSize(16).text(info.userName)
        resetTextStyle(doc)
        doc.text(timeUtils.formatDate(dayjs()), 400, 125, { align: 'right', width: 163 })
      })
    )

    // visual separator
    const separatorStruct = doc.struct('Artifact', { type: 'Layout' }, () => {
      doc.fillColor(lightGray)
      doc.markContent('Artifact', { type: 'Layout' })
      const startX = 70
      const width = doc.page.width - startX - margin
      doc.rect(startX, doc.y - lineGap, width, 1)
      doc.fill()
      resetTextStyle(doc)
    })
    titlePageHeader.add(separatorStruct)

    titlePageHeader.end()
  }

  const addOverallStats = (doc, overallTranscript, info, labelsConf, shouldAddTitle = true) => {
    const overallStats = doc.struct('Sect', { title: `Overall Stats` })
    overallTranscript.add(overallStats)
    if (shouldAddTitle) {
      addTitle(doc, overallStats, 'Progress Snapshot', 165)
    }

    const addStat = (label, icon, x, num, totalNum = null) => {
      const currentY = doc.y
      return doc.struct('Div', { title: `${label} stat` }, [
        doc.struct('Span', { alt: `${label} stat icon` }, () => {
          doc.image(icon, x, currentY - 2, { fit: [16, 16] })
        }),
        doc.struct('Span', () => {
          doc.text(`${label}: `, x + 20, currentY, { continued: true })
            .fillColor(arrowColor5).fontSize(13)
            .text(`${numFormat.pretty(num)}`, { continued: totalNum !== null })
          resetTextStyle(doc)
          if (totalNum !== null) {
            doc.text(` / ${numFormat.pretty(totalNum)}`, {})
          }
        })
      ])
    }
    overallStats.add(addStat(labelsConf.level, base64Images.trophy, 50, info.userLevel, info.totalLevels))
    doc.moveUp()
    overallStats.add(addStat(`${labelsConf.skill}s`, base64Images.arrowUp, 300, info.userSkillsCompleted, info.totalSkills))
    overallStats.add(addStat('Points', base64Images.hat, 50, info.userPoints, info.totalPoints))
    if (info.achievedBadges) {
      doc.moveUp()
      overallStats.add(addStat(`${labelsConf.badge}s`, base64Images.badge, 300, info.achievedBadges.length))
    }
    overallStats.end()
  }

  const addTitle = (doc, section, title, y = null) => {
    doc.moveDown(1)
    section.add(
      doc.struct('H', () => {
        doc.fontSize(15).fillColor(darkGreen).text(title.toUpperCase(), 50, y)
      })
    )
    resetTextStyle(doc)
  }

  const download = (doc, info) => {
    const filename = `${info.projectName} - ${info.userName} - Transcript.pdf`

    // Write the PDF content to a buffer
    const buffer = []
    doc.on('data', (chunk) => {
      buffer.push(chunk)
    })
    doc.on('end', () => {
      // Create a blob from the buffer
      const blob = new Blob(buffer, { type: 'application/pdf' })

      // Create a downloadable link
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      link.click()

      // Clean up
      URL.revokeObjectURL(link.href)
    })
    doc.end()
  }

  return {
    generatePdf
  }
}