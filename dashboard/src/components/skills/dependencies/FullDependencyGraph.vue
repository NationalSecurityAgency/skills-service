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
import { computed, onMounted, ref, useTemplateRef } from 'vue'
import { useRoute } from 'vue-router'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import PrerequisiteSelector from '@/components/skills/dependencies/PrerequisiteSelector.vue'
import DependencyTable from '@/common-components/dependency/DependencyTable.vue'
import ShareSkillsWithOtherProjects from '@/components/skills/crossProjects/ShareSkillsWithOtherProjects.vue'
import SharedSkillsFromOtherProjects from '@/components/skills/crossProjects/SharedSkillsFromOtherProjects.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import DependencyGraph from '@/common-components/dependency/DependencyGraph.vue'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'

const route = useRoute()
const projConfig = useProjConfig()
const dialogMessages = useDialogMessages()
const graphComponent = useTemplateRef('graphComponent')
const graph = ref({ nodes: [], edges: [] })
const graphData = ref({})
const isLoading = ref(true)
const selectedFromSkills = ref({})
const active = ref(null)
const isFullscreen = ref(false)
const isEditable = computed(() => !projConfig.isReadOnlyProj)
const mode = "admin"

const loadGraph = (addedNode = null) => {
  isLoading.value = true
  return SkillsService.getDependentSkillsGraphForProject(route.params.projectId)
    .then((response) => {
      graph.value = response
      if (addedNode) graphComponent.value?.refresh(addedNode)
    })
    .finally(() => { isLoading.value = false })
}
const clearSelectedFromSkills = () => { selectedFromSkills.value = {} }
const updateSelectedFromSkills = (item) => { selectedFromSkills.value = item || {} }
const handleUpdate = (addedNode) => {
  clearSelectedFromSkills()
  loadGraph(addedNode)
}
const removeDependency = ({ fromNode, toNode }) => {
  dialogMessages.msgConfirm({
    message: `Do you want to remove the path from ${fromNode.name} to ${toNode.name}?`,
    header: 'Remove Learning Path?',
    acceptLabel: 'Remove',
    rejectLabel: 'Cancel',
    appendTo: '#dependency-graph',
    accept: () => SkillsService.removeDependency(toNode.projectId, toNode.skillId, fromNode.skillId, fromNode.projectId)
      .then(() => handleUpdate()),
  })
}
onMounted(loadGraph)
</script>

<template>
  <div id="full-dependent-skills-graph">
    <SubPageHeader title="Learning Path" class="m-5" />
    <PrerequisiteSelector
      v-if="isEditable && !isFullscreen"
      :project-id="route.params.projectId"
      :disabled="isLoading"
      :selected-from-skills="selectedFromSkills"
      :show-header="true"
      @before-update="isLoading = true"
      @update="handleUpdate"
      @update-selected-from-skills="updateSelectedFromSkills"
      @clear-selected-from-skills="clearSelectedFromSkills" />
    <DependencyGraph
      ref="graphComponent"
      :graph="graph"
      :is-loading="isLoading"
      :project-id="route.params.projectId"
      :editable="isEditable"
      :mode="mode"
      @data-changed="graphData = $event"
      @fullscreen-changed="isFullscreen = $event; clearSelectedFromSkills()"
      @node-selected="updateSelectedFromSkills"
      @edge-selected="removeDependency">
      <template #fullscreen-header="{ isFullscreen }">
        <Accordion v-if="isFullscreen && isEditable" v-model:value="active" id="prerequisiteContent">
          <AccordionPanel value="0">
            <AccordionHeader>Add a new item to the learning path</AccordionHeader>
            <AccordionContent>
              <PrerequisiteSelector
                :show-header="false"
                :project-id="route.params.projectId"
                :disabled="isLoading"
                :selected-from-skills="selectedFromSkills"
                append-to="self"
                @before-update="isLoading = true"
                @update="handleUpdate"
                @update-selected-from-skills="updateSelectedFromSkills"
                @clear-selected-from-skills="clearSelectedFromSkills" />
            </AccordionContent>
          </AccordionPanel>
        </Accordion>
      </template>
    </DependencyGraph>
    <DependencyTable
      v-if="graph.nodes.length"
      :is-loading="isLoading"
      :data="graphData"
      :editable="isEditable"
      :mode="mode"
      :project-id="route.params.projectId"
      @update="handleUpdate"
      @pan-to-node="graphComponent?.panToNode" />
    <ShareSkillsWithOtherProjects v-if="isEditable" :project-id="route.params.projectId" />
    <SharedSkillsFromOtherProjects v-if="isEditable" :project-id="route.params.projectId" />
  </div>
</template>
