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
import { computed, nextTick, ref, watch } from 'vue'
import { useStorage } from '@vueuse/core'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsService from '@/components/skills/SkillsService.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'
import { useDependencyNavigation } from '@/common-components/dependency/UseDependencyNavigation.js'
import { RouterLink, useRoute } from 'vue-router'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const dialogMessages = useDialogMessages()
const props = defineProps({
  isLoading: Boolean,
  data: Object,
  editable: { type: Boolean, default: false },
  mode: { type: String, default: 'admin' },
  projectId: { type: String, required: true },
})
const emit = defineEmits(['update', 'panToNode'])
const announcer = useSkillsAnnouncer()
const dependencyNavigation = useDependencyNavigation()
const themeState = useSkillsDisplayThemeState()
const route = useRoute()
const routeHeaderStyle = computed(() => props.mode === 'skills-display' ? {
  color: themeState.graphTextPrimaryColor || undefined,
  backgroundColor: themeState.theme?.tiles?.backgroundColor || undefined,
} : {})
const paginatorLabelStyle = computed(() => props.mode === 'skills-display' ? {
  color: themeState.graphTextPrimaryColor || undefined,
} : {})

const learningPaths = ref([])
const pageSize = useStorage('dependencies-pageSize', 5)
const sortField = ref('')
const sortOrder = ref(0)

const getNode = (nodes, id) => {
  const foundNode = nodes.get({
    filter: (node) => {
      return node.id === id && node.type !== 'Badge-Skills'
    }
  })
  if(foundNode && foundNode.length > 0) {
    return foundNode[0];
  } else {
    return null;
  }
}

watch(() => props.data, () => {
  const learningPathsTmp = []
  if (props.data?.edges?.length) {
    const {nodes, edges} = props.data
    edges.forEach((edge) => {
      const fromNode = getNode(nodes, edge.from)
      const toNode = getNode(nodes, edge.to)

      if (fromNode && toNode) {
        learningPathsTmp.push({
          fromItem: fromNode?.details?.name,
          fromNode: fromNode?.details,
          fromAchieved: !!fromNode?.details?.achieved,
          toItem: toNode?.details?.name,
          toNode: toNode?.details,
          toAchieved: !!toNode?.details?.achieved
        })
      }
    })
  }
  learningPaths.value = learningPathsTmp
}, { immediate: true })

const removeLearningPath = (data) => {
  const message = `Do you want to remove the path from ${data.fromItem} to ${data.toItem}?`
  dialogMessages.msgConfirm({
    message: message,
    header: 'Remove Learning Path',
    acceptLabel: 'Remove',
    rejectLabel: 'Cancel',
    accept: () => {
      SkillsService.removeDependency(data.toNode.projectId, data.toNode.skillId, data.fromNode.skillId, data.fromNode.projectId).then(() => {
        emit('update')
      }).finally(() => {
        nextTick(() => announcer.assertive(`Successfully removed Learning Path route of ${data.fromItem} to ${data.toItem}`))
      })
    }
  })
}

const getRoute = (item) => dependencyNavigation.getRoute(item, props.mode === 'skills-display')

const sortTable = (criteria) => {
  sortField.value = criteria.sortField
  sortOrder.value = criteria.sortOrder
}

const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.sm.value)

const jumpToNode = (value) => {
  emit('panToNode', value.fromNode.id, true)
}
</script>

<template>
  <Card class="mb-4" :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Learning Path Routes" :title-tag="mode === 'skills-display' ? 'h2' : 'h3'" :style="routeHeaderStyle" />
    </template>
    <template #content>
      <div v-if="learningPaths.length > 0">
        <SkillsDataTable
          tableStoredStateId="dependencies"
          aria-label="Learning Path Routes"
          :value="learningPaths"
          :loading="isLoading"
          data-cy="learningPathTable"
          paginator :rows="pageSize" :rowsPerPageOptions="[5, 10, 15, 20]"
          @page="pageSize = $event.rows"
          show-gridlines
          :sortField="sortField"
          :sortOrder="sortOrder"
          @sort="sortTable"
          striped-rows>
          <Column field="fromItem" header="From" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <div>
                <RouterLink :to="getRoute(slotProps.data.fromNode)" :data-cy="`fromNodeLink_${slotProps.data.fromNode.skillId}`">{{ slotProps.data.fromItem }}</RouterLink>
                <span v-if="slotProps.data.fromNode.projectId !== projectId" class="block italic text-sm pl-2">shared from {{ slotProps.data.fromNode.projectName }}</span>
              </div>
            </template>
          </Column>
          <Column v-if="mode === 'skills-display'" field="fromAchieved" header="From Status" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <span :data-cy="`fromNodeStatus_${slotProps.data.fromNode.skillId}`" :style="slotProps.data.fromAchieved ? { color: themeState.graphAchievedColor } : undefined"><i v-if="slotProps.data.fromAchieved" class="fas fa-check mr-1" aria-hidden="true" />{{ slotProps.data.fromAchieved ? 'Achieved' : 'Not achieved' }}</span>
            </template>
          </Column>
          <Column field="toItem" header="To" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <div>
                <RouterLink :to="getRoute(slotProps.data.toNode)" :data-cy="`toNodeLink_${slotProps.data.toNode.skillId}`">{{ slotProps.data.toItem }}</RouterLink>
                <span v-if="slotProps.data.toNode.projectId !== projectId" class="block italic text-sm pl-2">shared from {{ slotProps.data.toNode.projectName }}</span>
              </div>
            </template>
          </Column>
          <Column v-if="mode === 'skills-display'" field="toAchieved" header="To Status" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <span :data-cy="`toNodeStatus_${slotProps.data.toNode.skillId}`" :style="slotProps.data.toAchieved ? { color: themeState.graphAchievedColor } : undefined"><i v-if="slotProps.data.toAchieved" class="fas fa-check mr-1" aria-hidden="true" />{{ slotProps.data.toAchieved ? 'Achieved' : 'Not achieved' }}</span>
            </template>
          </Column>
          <Column field="edit" header="View Route" v-if="editable" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <SkillsButton @click="jumpToNode(slotProps.data)" variant="outline-info" size="small" class="text-info mr-2" icon="fa fa-network-wired"
                            :aria-label="`View route of ${slotProps.data.fromItem} to ${slotProps.data.toItem} in graph`" title="View route in graph"></SkillsButton>
            </template>
          </Column>
          <Column field="edit" header="Edit" v-if="editable" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <SkillsButton @click="removeLearningPath(slotProps.data)"
                      variant="outline-info" size="small" class="text-info" icon="fa fa-trash"
                      :track-for-focus="true" :id="`removeLearningPathButton-${slotProps.data.fromItem}-${slotProps.data.toItem}`"
                      :aria-label="`Remove learning path route of ${slotProps.data.fromItem} to ${slotProps.data.toItem}`"
                      data-cy="sharedSkillsTable-removeBtn"></SkillsButton>
            </template>
          </Column>

          <template #paginatorstart>
            <span data-cy="learningPathTotalRows" :style="paginatorLabelStyle">Total Rows: <span class="font-semibold" data-cy="skillsBTableTotalRows">{{ learningPaths.length }}</span></span>
          </template>
        </SkillsDataTable>
      </div>
      <div v-else>
        <no-content2 title="No Learning Paths Yet..." icon="fas fa-share-alt" class="my-8"
                     message="Add a path between a Skill/Badge and another Skill/Badge" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>
