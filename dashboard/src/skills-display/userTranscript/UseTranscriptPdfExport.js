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
import { jsPDF } from 'jspdf'
import autoTable from 'jspdf-autotable'
import { useBase64Images } from '@/skills-display/userTranscript/UseBase64Images.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import dayjs from '@/common-components/DayJsCustomizer'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

export const useTranscriptPdfExport = () => {

  const base64Images = useBase64Images()
  const numFormat = useNumberFormat()
  const timeUtils = useTimeUtils()
  const arrowColor1 = '#264653'
  const arrowColor2 = '#2a9d8f'
  const arrowColor5 = '#e76f51'

  const leftPageMargin = 15
  const fontSize = 12
  const fontSizeTitle1 = 14
  const generatePdf = (info) => {
    const doc = new jsPDF()
    doc.addImage(base64Images.logoArrows, 'PNG', leftPageMargin, 15, 8, 22)
    doc.setTextColor(arrowColor2)
    doc.setFontSize(20)
    doc.text('SkillTree TRANSCRIPT', 27, 20)

    doc.setFontSize(17)
    doc.setTextColor(arrowColor1)
    doc.text(info.projectName, 27, 29)
    doc.setFontSize(16)
    doc.text(info.userName, 27, 35)

    doc.setFontSize(fontSize)
    doc.text(timeUtils.formatDate(dayjs()), 163, 18)

    addProgressStats(doc, info)
    addSubjTable(doc, info)
    addBadgeTable(doc, info)

    const { labelsConf } = info
    for (let i = 0; i < info.subjects.length; i++) {
      const subject = info.subjects[i]
      if (subject.skills) {
        doc.addPage()
        addTitle(doc, subject.name, 30)


        const body = []
        for (let i = 0; i < subject.skills.length; i++) {
          const skill = subject.skills[i]
          body.push([
            subject.name,
            buildNumOutOfOtherNum(skill.userPoints, skill.totalPoints),
          ])
        }

        autoTable(doc, {
          head: [[`${labelsConf.skill}`, 'Points']],
          body,
          startY: 35
        })
      }
    }
    doc.save(`${info.projectName} - ${info.userName} - Transcript.pdf`)
  }

  const buildNumOutOfOtherNum = (num, otherNum) => {
    return `${num} / ${otherNum}`
  }
  const addProgressStats = (doc, info) => {
    const levelY = 43
    let starX = leftPageMargin
    for (let i = 1; i <= info.totalLevels; i++) {
      const image = i <= info.userLevel ? base64Images.filledStar : base64Images.star
      doc.addImage(image, 'PNG', starX, levelY, 8, 8, `star-${i + 1}`)
      starX += 10
    }
    addSingleProgressStat(doc, 'Level', leftPageMargin, levelY + 13, info.userLevel, info.totalLevels)
    addSingleProgressStat(doc, 'Points', leftPageMargin, levelY + 19, info.userPoints, info.totalPoints)

    const secondColumnXAdjustment = 145
    addSingleProgressStat(doc, 'Skills', leftPageMargin + secondColumnXAdjustment, levelY + 13, info.userSkillsCompleted, info.totalSkills)
    if (info.achievedBadges) {
      addSingleProgressStat(doc, 'Badges', leftPageMargin + secondColumnXAdjustment, levelY + 19, info.achievedBadges.length, null)
    }
  }
  const addSingleProgressStat = (doc, label, x, y, num, totalNum) => {
    doc.text(`${label}:`, x, y)
    doc.setFontSize(fontSize + 1)
    doc.setTextColor(arrowColor5)
    doc.text(numFormat.pretty(num), x + 18, y)
    doc.setFontSize(fontSize)
    doc.setTextColor(arrowColor1)
    if (totalNum) {
      doc.text(`/ ${numFormat.pretty(totalNum)}`, x + 28, y)
    }
  }

  const addTitle = (doc, title, y) => {
    doc.setFontSize(fontSizeTitle1)
    doc.setTextColor(arrowColor2)
    console.log(`adding title ${title} at y=${y}`)
    doc.text(title, leftPageMargin, y)
    doc.setFontSize(fontSize)
    doc.setTextColor(arrowColor1)
  }
  const addSubjTable = (doc, info) => {
    const startY = 75
    const { labelsConf } = info

    addTitle(doc, `${labelsConf.subject} Progress`.toUpperCase(), startY)

    const body = []
    for (let i = 0; i < info.subjects.length; i++) {
      const subject = info.subjects[i]
      body.push([
        subject.name,
        buildNumOutOfOtherNum(subject.userLevel, subject.totalLevels),
        buildNumOutOfOtherNum(subject.userPoints, subject.totalPoints),
        buildNumOutOfOtherNum(subject.userSkillsCompleted, subject.totalSkills)
      ])
    }

    autoTable(doc, {
      head: [[`${labelsConf.subject}`, 'Level', 'Points', 'Skills']],
      body,
      startY: startY + 3
    })
  }

  const addBadgeTable = (doc, info) => {
    if (info.achievedBadges) {
      const startY = 140
      const { labelsConf } = info

      addTitle(doc, `Earned ${labelsConf.badge}s`.toUpperCase(), startY)

      const body = []
      for (let i = 0; i < info.achievedBadges.length; i++) {
        const badge = info.achievedBadges[i]
        body.push([
          badge.name,
          badge.dateAchieved
        ])
      }

      autoTable(doc, {
        head: [[`${labelsConf.badge}`, 'Date Achieved']],
        body,
        // margin: { top: (startY+100) }
        startY: startY + 3
      })
    }
  }

  return {
    generatePdf
  }
}