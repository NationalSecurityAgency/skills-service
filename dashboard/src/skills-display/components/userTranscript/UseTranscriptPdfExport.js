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
import { usePdfBuilderHelper } from '@/skills-display/components/userTranscript/UsePdfBuilderHelper.js'

export const useTranscriptPdfExport = () => {

  const base64Images = useBase64Images()
  const numFormat = useNumberFormat()
  const timeUtils = useTimeUtils()
  const tableBuilder = useTableBuilder()
  const pdfHelper = usePdfBuilderHelper()


  const margin = 50
  const lineGap = 3
  const pageStartY = 70

  const buildNumOutOfOtherNum = (num, otherNum, addAchievedWord = true) => {
    let res = `${numFormat.pretty(num)} / ${numFormat.pretty(otherNum)}`
    return res
  }

  const generatePdf = (info) => {
    const doc = new PDFDocument({
      pdfVersion: '1.5',
      lang: 'en-US',
      tagged: true,
      displayTitle: true,
      bufferPages: true,
      margin: 10
    })
    doc.pageNum = 1
    doc.lineGap(lineGap)
    setDocMetaData(doc, info)

    // Document
    const overallTranscript = doc.struct('Document')
    doc.addStructure(overallTranscript)

    pdfHelper.addHeader(doc, overallTranscript, info)

    // Transcript -> header
    addTitleSection(doc, overallTranscript, info)
    addOverallStats(doc, overallTranscript, info, info.labelsConf)
    addSubjectsProgress(doc, overallTranscript, info)
    pdfHelper.addFooter(doc, overallTranscript, info)

    // Transcript -> subject pages
    addSubjectPagesOfSkills(doc, overallTranscript, info)

    // Transcript -> earned badge
    addEarnBadges(doc, overallTranscript, info)

    download(doc, info)
  }

  const setDocMetaData = (doc, info) => {
    // Set some meta data
    doc.info['Title'] = `SkillTree ${info.projectName} Transcript for ${info.userName}`
    doc.info['Author'] = 'SkillTree'
    doc.info['Subject'] = 'Project Transcript'
    doc.info['Keywords'] = 'skilltree,transcript,pdf,project'
  }


  const addSubjectsProgress = (doc, overallTranscript, info) => {
    if (info.subjects && info.subjects.length > 0) {
      const subjectsProgress = doc.struct('Sect', { title: `Subjects Progress` })
      overallTranscript.add(subjectsProgress)
      pdfHelper.addTitle(doc, subjectsProgress, 'Subjects Progress')

      const tableInfo = {
        title: 'Subjects Progress Table',
        headers: [`${info.labelsConf.subject}`, `${info.labelsConf.level}`, `${info.labelsConf.point}s`, `${info.labelsConf.skill}s`],
        rows: info.subjects.map((subject) => {
          return [
            subject.name,
            buildNumOutOfOtherNum(subject.userLevel, subject.totalLevels, false),
            buildNumOutOfOtherNum(subject.userPoints, subject.totalPoints, false),
            buildNumOutOfOtherNum(subject.userSkillsCompleted, subject.totalSkills, false)
          ]
        })
      }
      tableBuilder.addTable(doc, overallTranscript, subjectsProgress, tableInfo)

      subjectsProgress.end()
    }
  }

  const addEarnBadges = (doc, overallTranscript, info) => {
    if (info.achievedBadges && info.achievedBadges.length > 0) {
      doc.addPage({ margin: 10 })
      doc.pageNum++
      pdfHelper.addHeader(doc, overallTranscript, info)

      const earnedBadgesStruct = doc.struct('Sect', { title: `Earned Badges` })
      overallTranscript.add(earnedBadgesStruct)
      pdfHelper.addTitle(doc, earnedBadgesStruct, 'Earned Badges', pageStartY)

      const tableInfo = {
        headerAndFooter: info.headerAndFooter,
        structTitle: 'Earned Badges Table',
        sectionTitle: 'Earned Badges Table',
        headers: [`${info.labelsConf.badge}`, 'Achieved On'],
        rows: info.achievedBadges.map((badge) => {
          return [
            badge.name,
            dayjs(badge.dateAchieved).format('YYYY-MM-DD')
          ]
        })
      }
      doc.moveDown(0.5)

      tableBuilder.addTable(doc, overallTranscript, earnedBadgesStruct, tableInfo)

      earnedBadgesStruct.end()

      pdfHelper.addFooter(doc, overallTranscript, info)
    }
  }

  const addSubjectPagesOfSkills = (doc, overallTranscript, info) => {
    info.subjects.forEach((subject) => {
      if (subject.skills && subject.skills.length > 0) {
        doc.addPage({ margin: 10 })
        subject.pageNum = doc.pageNum
        pdfHelper.addHeader(doc, overallTranscript, info)

        const subjectStruct = doc.struct('Sect', { title: `${subject.name} Subject` })
        overallTranscript.add(subjectStruct)
        doc.pageNum++

        const sectionTitle = `Subject: ${subject.name}`
        pdfHelper.addTitle(doc, subjectStruct, sectionTitle, pageStartY)

        addOverallStats(doc, subjectStruct, subject, info.labelsConf, false)

        const hasApprovals = subject.skills.find(skill => skill.approvedBy !== "")

        const headerRow = [`${info.labelsConf.skill}`, `${info.labelsConf.point}s`, 'Achieved On'];
        if(hasApprovals) {
          headerRow.push('Approver');
        }
        const tableInfo = {
          headerAndFooter: info.headerAndFooter,
          structTitle: `Skill Progress Table for ${subject.name} subject`,
          sectionTitle,
          headers: headerRow,
          rows: subject.skills.map((skill) => {
            const skillRow = [
              skill.name,
              buildNumOutOfOtherNum(skill.userPoints, skill.totalPoints),
              skill.achievedOn ? skill.achievedOn : '',
            ]
            if(hasApprovals) {
              skillRow.push(skill.approvedBy ? skill.approvedBy : '',)
            }
            return skillRow;
          })
        }
        doc.moveDown(0.5)
        tableBuilder.addTable(doc, overallTranscript, subjectStruct, tableInfo)
        pdfHelper.addFooter(doc, overallTranscript, info)
      }
    })
  }

  const addTitleSection = (doc, overallTranscript, info) => {
    const titlePageHeader = doc.struct('Sect', { title: 'Transcript Title Section' })
    overallTranscript.add(titlePageHeader)

    titlePageHeader.add(
      doc.struct('Span', { alt: 'SkillTree Logo Arrows Image' }, () => {
        doc.image(base64Images.logoArrows, 42, 73, { fit: [25, 60] })
      })
    )
    titlePageHeader.add(
      doc.struct('H', () => {
        doc.fontSize(20).fillColor(pdfHelper.darkGreen).text('SkillTree Transcript ', 73, pageStartY)
      })
    )
    pdfHelper.resetTextStyle(doc)

    titlePageHeader.add(
      doc.struct('Span', { alt: 'SkillTree Logo Image' }, () => {
        doc.image(base64Images.logo, 470, 68, { fit: [90, 50] })
      })
    )

    titlePageHeader.add(
      doc.struct('P', () => {
        doc.fontSize(17).text(`${info.projectName} `)
        doc.fontSize(16).text(`${info.userName} `)
        pdfHelper.resetTextStyle(doc)
        doc.text(`${timeUtils.formatDate(dayjs())} `, 400, 125, { align: 'right', width: 163 })
      })
    )

    // visual separator
    const separatorStruct = doc.struct('Artifact', { type: 'Layout' }, () => {
      doc.fillColor(pdfHelper.lightGray)
      doc.markContent('Artifact', { type: 'Layout' })
      const startX = 70
      const width = doc.page.width - startX - margin
      doc.rect(startX, doc.y - lineGap, width, 1)
      doc.fill()
      pdfHelper.resetTextStyle(doc)
    })
    titlePageHeader.add(separatorStruct)

    titlePageHeader.end()
  }

  const addOverallStats = (doc, overallTranscript, info, labelsConf, shouldAddTitle = true) => {
    const overallStats = doc.struct('Sect', { title: `Overall Stats` })
    overallTranscript.add(overallStats)
    if (shouldAddTitle) {
      pdfHelper.addTitle(doc, overallStats, 'Progress Snapshot', 165)
    }

    const addStat = (label, icon, x, num, totalNum = null) => {
      const currentY = doc.y
      return doc.struct('Div', { title: `${label} stat` }, [
        doc.struct('Span', { alt: `${label} stat icon` }, () => {
          doc.image(icon, x, currentY - 2, { fit: [16, 16] })
        }),
        doc.struct('Span', () => {
          doc.text(`${label}: `, x + 20, currentY, { continued: true })
            .fillColor(pdfHelper.arrowColor5).fontSize(13)
            .text(`${numFormat.pretty(num)} `, { continued: totalNum !== null })
          pdfHelper.resetTextStyle(doc)
          if (totalNum !== null) {
            doc.text(`/ ${numFormat.pretty(totalNum)} `, {})
          }
        })
      ])
    }
    overallStats.add(addStat(labelsConf.level, base64Images.trophy, 50, info.userLevel, info.totalLevels))
    doc.moveUp()
    overallStats.add(addStat(`${labelsConf.skill}s`, base64Images.arrowUp, 300, info.userSkillsCompleted, info.totalSkills))
    overallStats.add(addStat(`${labelsConf.point}s`, base64Images.hat, 50, info.userPoints, info.totalPoints))
    if (info.achievedBadges && info.achievedBadges.length > 0) {
      doc.moveUp()
      overallStats.add(addStat(`${labelsConf.badge}s`, base64Images.badge, 300, info.achievedBadges.length))
    }
    overallStats.end()
  }


  const download = (doc, info) => {
    const cleanUpRegex = /[^a-zA-Z0-9@.()\s]/g
    const cleanProjName = info.projectName.replace(cleanUpRegex, '');
    const cleanUserName = info.userName.replace(cleanUpRegex, '');
    const filename = `${cleanProjName} - ${cleanUserName} - Transcript.pdf`

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