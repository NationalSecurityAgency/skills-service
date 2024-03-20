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