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
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import DependencyGraph from '@/common-components/dependency/DependencyGraph.vue'
import DependencyTable from '@/common-components/dependency/DependencyTable.vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const skillsDisplayService = useSkillsDisplayService()
const attributes = useSkillsDisplayAttributesState()
const graph = ref({ nodes: [], edges: [] })
const graphData = ref({})
const isLoading = ref(true)
const mode = "skills-display"

onMounted(() => {
  skillsDisplayService.getSkillDependenciesGraphForProject()
    .then((response) => { graph.value = response })
    .finally(() => { isLoading.value = false })
})
</script>

<template>
  <div>
    <SkillsTitle>Learning Path</SkillsTitle>
    <div class="mt-4">
      <DependencyGraph
        :graph="graph"
        :is-loading="isLoading"
        :project-id="attributes.projectId"
        :mode="mode"
        @data-changed="graphData = $event"
      />
      <DependencyTable v-if="graph.nodes.length" :is-loading="isLoading" :data="graphData" :mode="mode" :project-id="attributes.projectId"/>
    </div>
  </div>
</template>
