<script setup>
import { onMounted, ref } from 'vue'
import SkillsService from '@/components/skills/SkillsService.js'

const props = defineProps({
  projectId: {
    type: String,
    required: true
  },
  skillId: {
    type: String,
    required: true
  },
  linkLabel: {
    type: String,
    default: null
  }
})
const loading = ref(true)

const skill = ref({})
onMounted(() => {
  SkillsService.getSkillInfo(props.projectId, props.skillId)
    .then((res) => {
      skill.value = res
      loading.value = false;
    })
})
</script>

<template>
  <div class="inline-block">
    <skills-spinner :is-loading="loading" :size-in-rem="1"/>
    <router-link v-if="!loading"
                 :to="{ name:'SkillOverview', params: { projectId: projectId, subjectId: skill.subjectId, skillId:  skill.skillId }}"
                 :aria-label="`Navigate to skill ${skill.name}  via link`">
      <div class="d-inline-block" style="text-decoration: underline">
        <span v-if="linkLabel">{{ linkLabel }}</span>
        <span v-else>{{ skill.name }}</span>
      </div>
    </router-link>
  </div>
</template>

<style scoped>

</style>