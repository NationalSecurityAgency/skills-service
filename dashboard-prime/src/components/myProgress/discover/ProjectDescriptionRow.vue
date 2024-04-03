<script setup>
import ProjectService from '@/components/projects/ProjectService.js'
import { ref, onMounted } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'

const props = defineProps({
  projectId: {
    type: String,
    required: true
  }
})

const loadingDescription = ref(true)
const description = ref('')
const loadDescription = () => {
  loadingDescription.value = true
  ProjectService.loadDescription(props.projectId)
    .then((data) => {
      description.value = data.description
    })
    .finally(() => {
      loadingDescription.value = false
    })
}

onMounted(() => {
  loadDescription()
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="loadingDescription" />
    <div v-if="!loadingDescription">
      <markdown-text v-if="description" :text="description" />
      <div v-else>Project does not have a description</div>
    </div>
  </div>
</template>

<style scoped>

</style>