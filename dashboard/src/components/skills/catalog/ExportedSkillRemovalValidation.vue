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
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import { useRoute } from 'vue-router'
import ExportedSkillDeletionWarning from '@/components/skills/catalog/ExportedSkillDeletionWarning.vue'

const route = useRoute()
const props = defineProps({
  skillToRemove: Object,
})

const loading = ref(true)
const loadedStats = ref(null)

const loadData = () => {
  loading.value = true;
  CatalogService.getExportedStats(route.params.projectId, props.skillToRemove.skillId)
    .then((res) => {
      loadedStats.value = res;
    })
    .finally(() => {
      loading.value = false;
    });
}
onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="loading" class="mb-5" />
    <div v-if="!loading">
      <exported-skill-deletion-warning :loaded-stats="loadedStats"
                                       :skill-name="skillToRemove.skillName" />
    </div>
  </div>
</template>

<style scoped>

</style>