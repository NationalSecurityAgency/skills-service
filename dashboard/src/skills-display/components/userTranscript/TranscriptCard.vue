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
import { computed } from 'vue'
import { useTranscriptPdfExport } from '@/skills-display/components/userTranscript/UseTranscriptPdfExport.js'
import { useLoadTranscriptData } from '@/skills-display/components/userTranscript/UseLoadTranscriptData.js'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'

const loadTranscriptData = useLoadTranscriptData()
const transcriptPdfExport = useTranscriptPdfExport()
const userProgressSummaryState = useUserProgressSummaryState()
const plural = useLanguagePluralSupport()

const totalSkills = computed(() => userProgressSummaryState.userProgressSummary?.totalSkills || 0)
const skillsAchieved = computed(() => userProgressSummaryState.userProgressSummary?.skillsAchieved || 0)

const exportTranscriptToPdf = () => {
  loadTranscriptData.loadTranscriptData().then((transcriptInfo) => {
    transcriptPdfExport.generatePdf(transcriptInfo)
  })
}
</script>

<template>
  <Card :pt="{ content: { class: 'p-0' } }" data-cy="downloadTranscriptCard">
    <template #content>
      <div class="flex flex-col md:flex-row gap-2 items-center">
        <div class="flex-1">
          You have Completed
          <Tag>{{ skillsAchieved }}</Tag>
          out of <Tag severity="secondary">{{ totalSkills }}</Tag> skill{{ plural.plural(totalSkills) }}!
        </div>
        <div>
          <SkillsButton
            icon="fas fa-download"
            label="Download Transcript"
            :loading="loadTranscriptData.isLoading.value"
            @click="exportTranscriptToPdf"
            data-cy="downloadTranscriptBtn" />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>