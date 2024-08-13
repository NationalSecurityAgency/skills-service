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
import { onMounted, ref } from 'vue'
import { useLoadTranscriptData } from '@/skills-display/components/userTranscript/UseLoadTranscriptData.js'
import { useTranscriptPdfExport1 } from '@/skills-display/components/userTranscript/UseTranscriptPdfExport1.js'

const loadTranscriptData = useLoadTranscriptData()
const transcriptPdfExport = useTranscriptPdfExport1(0)
const pdfViewer = ref(null)
onMounted(() => {
  loadTranscriptData.loadTranscriptData().then((transcriptInfo) => {
    transcriptPdfExport.generatePdf(transcriptInfo, pdfViewer.value)
  })
})


</script>

<template>
<div>
  <H1>Transcript</H1>
  <skills-spinner :is-loading="loadTranscriptData.isLoading.value" class="mt-8"/>
  <iframe ref="pdfViewer" width="600" height="775"></iframe>
</div>
</template>

<style scoped>

</style>