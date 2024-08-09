/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { useTranscriptPdfExport } from '@/skills-display/userTranscript/UseTranscriptPdfExport.js'

const transcriptInfo = {
  labelsConf: {
    subject: 'Subject',
    skill: 'Skill',
    level: 'Level',
    badge: 'Badge'
  },
  projectName: 'ABC Onboarding',
  userName: 'John Doe',
  userLevel: 1,
  totalLevels: 5,
  userPoints: 200,
  totalPoints: 1500,
  userSkillsCompleted: 3,
  totalSkills: 26,
  subjects: [{
    name: 'Subject A',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5,
    skills: [
      {
        name: 'Skill A',
        userPoints: 200,
        totalPoints: 1500
      },
      {
        name: 'Skill B',
        userPoints: 200,
        totalPoints: 1500
      }
    ]
  }, {
    name: 'Subject B',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5
  }, {
    name: 'Subject C',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5,
    skills: [
      {
        name: 'Skill A',
        userPoints: 200,
        totalPoints: 1500
      },
      {
        name: 'Skill B',
        userPoints: 200,
        totalPoints: 1500
      }
    ]
  }, {
    name: 'Subject D',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5
  }, {
    name: 'Subject E',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5
  }, {
    name: 'Subject F',
    userLevel: 1,
    totalLevels: 5,
    userPoints: 200,
    totalPoints: 1500,
    userSkillsCompleted: 3,
    totalSkills: 5
  }],
  achievedBadges: [
    {
      name: 'Badge 1',
      dateAchieved: '2020-01-01'
    }
  ]
}
const transcriptPdfExport = useTranscriptPdfExport()
const exportTranscriptToPdf = () => transcriptPdfExport.generatePdf(transcriptInfo)
</script>

<template>
  <Card :pt="{ content: { class: 'p-0' } }">
    <template #content>
      <div class="flex align-items-center">
        <div class="flex-1">
          You have Completed
          <Tag>10</Tag>
          out of 70 skills!
        </div>
        <div>
          <SkillsButton icon="fas fa-download" label="Download Transcript" @click="exportTranscriptToPdf" />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>