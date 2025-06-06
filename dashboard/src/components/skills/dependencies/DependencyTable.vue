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
import {computed, nextTick, onMounted, ref} from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsService from '@/components/skills/SkillsService'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const dialogMessages = useDialogMessages()
const projConfig = useProjConfig();
const props = defineProps(['isLoading', 'data'])
const emit = defineEmits(['update'])
const announcer = useSkillsAnnouncer()

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

const learningPaths = ref([])
const isProcessing = ref(true)
const sortField = ref('')
const sortOrder = ref(0)

onMounted(() => {
  if (props.data && props.data.edges && props.data.edges.length > 0) {
    const { nodes, edges } = props.data

    if (edges && edges.length > 0) {
      edges.forEach((edge) => {
        const fromNode = nodes.find((node) => node.id === edge.from && node.type !== 'Badge-Skills')
        const toNode = nodes.find((node) => node.id === edge.to && node.type !== 'Badge-Skills')

        if( fromNode && toNode ) {
          learningPaths.value.push({
            fromItem: fromNode?.details?.name,
            fromNode: fromNode?.details,
            toItem: toNode?.details?.name,
            toNode: toNode?.details
          })
        }
      })
    }
    isProcessing.value = false
  }
})

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

const getUrl = (item) => {
  let url = `/administrator/projects/${encodeURIComponent(item.projectId)}`
  if (item.type === 'Skill') {
    url += `/subjects/${encodeURIComponent(item.subjectId)}/skills/${encodeURIComponent(item.skillId)}/`
  } else if (item.type === 'Badge') {
    url += `/badges/${encodeURIComponent(item.skillId)}/`
  }

  return url
}

const sortTable = (criteria) => {
  sortField.value = criteria.sortField
  sortOrder.value = criteria.sortOrder
}

const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.sm.value)
</script>

<template>
  <Card class="mb-4" :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Learning Path Routes"></SkillsCardHeader>
    </template>
    <template #content>
      <div v-if="!isLoading && !isProcessing && learningPaths.length > 0">
        <SkillsDataTable
          tableStoredStateId="dependencies"
          aria-label="Learning Path Routes"
          :value="learningPaths"
          v-if="!isProcessing"
          data-cy="learningPathTable"
          paginator :rows="5" :rowsPerPageOptions="[5, 10, 15, 20]"
          show-gridlines
          :sortField="sortField"
          :sortOrder="sortOrder"
          @sort="sortTable"
          striped-rows>
          <Column field="fromItem" header="From" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <a :href="getUrl(slotProps.data.fromNode)">{{ slotProps.data.fromItem }}</a>
            </template>
          </Column>
          <Column field="toItem" header="To" sortable :class="{'flex': isFlex }">
            <template #body="slotProps">
              <a :href="getUrl(slotProps.data.toNode)">{{ slotProps.data.toItem }}</a>
            </template>
          </Column>
          <Column field="edit" header="Edit" v-if="!isReadOnlyProj" :class="{'flex': isFlex }">
            <template #body="slotProps">
              <SkillsButton @click="removeLearningPath(slotProps.data)"
                      variant="outline-info" size="small" class="text-info" icon="fa fa-trash"
                      :track-for-focus="true" :id="`removeLearningPathButton-${slotProps.data.fromItem}-${slotProps.data.toItem}`"
                      :aria-label="`Remove learning path route of ${slotProps.data.fromItem} to ${slotProps.data.toItem}`"
                      data-cy="sharedSkillsTable-removeBtn"></SkillsButton>
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ learningPaths.length
            }}</span>
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