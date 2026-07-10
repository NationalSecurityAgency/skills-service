/*
Copyright 2026 SkillTree

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
import {computed, onMounted, ref, watch} from "vue";
import {useRoute} from "vue-router";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import {useStorage} from "@vueuse/core";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import CreateTagDialog from "@/components/skills/tags/CreateTagDialog.vue";
import {useProjConfig} from "@/stores/UseProjConfig.js";
import RemovalValidation from "@/components/utils/modal/RemovalValidation.vue";
import {useFocusState} from "@/stores/UseFocusState.js";
import NoContent2 from "@/components/utils/NoContent2.vue";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import DateCell from "@/components/utils/table/DateCell.vue";

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const announcer = useSkillsAnnouncer()
const projConf = useProjConfig()
const focusState = useFocusState()

const tags = ref([])
const isInitialLoading = ref(true)
const isLoading = ref(false)
const sortInfo = ref({ sortOrder: -1, sortBy: 'createdOn' })
const possiblePageSizes = [10, 25, 50, 100]
const tableId = 'skillsTagsTable'
const pageSize = useStorage(`${tableId}-pageSize`, 25)
const filter = ref('')

const showCreateSkillTagDialog = ref(false)
const editExistingTagId = ref(null)

const editExistingTag = (tag) => {
  editExistingTagId.value = tag.tagId
  showCreateSkillTagDialog.value = true
}
watch(() => showCreateSkillTagDialog.value, (newVal) => {
  if (!newVal) {
    editExistingTagId.value = null
  }
})

const deleteTagInfo = ref({
  showDialog: false,
  tagId: null,
  tagName: null,
})

onMounted(() => {
  loadTags()
})

const hasData = computed(() => tags.value && tags.value.length > 0)

const loadTags = () => {
  if (!hasData.value) {
    isInitialLoading.value = true
  }

  isLoading.value = true
  return SkillsService.getTagsForProject(route.params.projectId)
      .then((data) => {
        tags.value = data;
      })
      .finally(() => {
        isLoading.value = false;
        isInitialLoading.value = false;
      })
}

const filteredTags = computed(() => {
  if (filter.value && filter.value.toString().trim().length > 0) {
    return tags.value.filter((tag) => tag?.tagValue?.toString().toLowerCase().includes(filter.value.toString().toLowerCase()) )
  }

  return tags.value
})

const numRows = computed(() => filteredTags.value.length)

const clearFilter = () => {
  filter.value = ''
  announcer.polite('Skills filter was reset. Showing all results')
}

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
}
const initiateRemoveTag = (tag) => {
  deleteTagInfo.value.tagId = tag.tagId
  deleteTagInfo.value.tagName = tag.tagValue
  deleteTagInfo.value.showDialog = true
}

const onTagAdded = (tagInfo) => {
  loadTags().then(() => {
    focusOnBtnThatInitiated(tagInfo.operation === 'edit' ? tagInfo.tagId : null)
  })
}

const removeTag = () => {
  isLoading.value = true
  const {tagId} = deleteTagInfo.value
  SkillsService.deleteTagForSkills(route.params.projectId, [], tagId, true)
      .then(() => {
            tags.value = tags.value.filter((item) => item.tagId !== tagId);
          }
      )
      .finally(() => {
        deleteTagInfo.value.tagName = null
        deleteTagInfo.value.tagId = null
        isLoading.value = false
        focusOnBtnThatInitiated()
      })
}

const focusOnBtnThatInitiated = (editTagId) => {
  const elementId = editTagId ? `editTag_${editTagId}` : 'actionButton'
  focusState.setElementId(elementId)
  focusState.focusOnLastElement()
}
</script>

<template>
  <div class="w-full">
    <sub-page-header
        title="Skill Tags"
        action="Tag"
        :disabled="isInitialLoading || isLoading"
        @add-action="showCreateSkillTagDialog = true"/>
    <skills-spinner v-if="isInitialLoading" :is-loading="isInitialLoading" class="my-14"/>
    <Card v-if="!isInitialLoading && hasData" :pt="{ 'body': 'p-0! m-0!'}">
      <template #content>
          <div class="p-4">
            <InputGroup>
              <InputGroupAddon>
                <i class="fas fa-search" aria-hidden="true"/>
              </InputGroupAddon>
              <InputText
                  class="flex grow"
                  v-model="filter"
                  :disabled="isLoading"
                  data-cy="tagsTable-skillFilter"
                  aria-label="Tag Filter"
                  placeholder="Tag Filter"/>
              <InputGroupAddon class="p-0 m-0">
                <SkillsButton
                    id="tagFilterClearBtn"
                    icon="fa fa-times"
                    text
                    outlined
                    @click="clearFilter"
                    :disabled="isLoading"
                    aria-label="Reset filter"
                    data-cy="filterResetBtn"/>
              </InputGroupAddon>
            </InputGroup>
          </div>
          <SkillsDataTable
              :tableStoredStateId="tableId"
              aria-label="Skill Tags"
              :value="filteredTags"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder"
              :loading="isLoading"
              paginator
              :rows="pageSize"
              :rowsPerPageOptions="possiblePageSizes"
              @page="pageChanged"
              data-cy="skillsTagsTable"
          >
            <Column field="tagValue" header="Tag" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-tag mr-1" :class="colors.getTextClass(0)" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <router-link
                    :id="slotProps.data.tagValue"
                    :to="{ name:'SkillTagSkills', params: { projectId: route.params.projectId, tagId: slotProps.data.tagId }}"
                    class="underline"
                    :data-cy="`manageTag_${slotProps.data.tagValue}`">
                  {{ slotProps.data.tagValue }}
                </router-link>
              </template>
            </Column>
            <Column field="numSkills" header="# Skills" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-graduation-cap mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
              </template>
            </Column>
            <Column field="createdOn" header="Created On" :sortable="true" :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-calendar mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <DateCell :value="slotProps.data.createdOn" />
              </template>
            </Column>
            <Column v-if="!projConf.isReadOnlyProj" style="width: 7rem"
                    :class="{'flex': responsive.md.value }">
              <template #body="slotProps">
                <ButtonGroup>
                  <SkillsButton :id="`editTag_${slotProps.data.tagId}`"
                                @click="editExistingTag(slotProps.data)"
                                size="small"
                                icon="fa-solid fa-edit"
                                :track-for-focus="true"
                                :data-cy="`editTag_${slotProps.data.tagId}`"
                                :aria-label="`edit tag ${slotProps.data.tagValue}`">
                  </SkillsButton>
                  <SkillsButton :id="`removeTag_${slotProps.data.tagId}`"
                                @click="initiateRemoveTag(slotProps.data)"
                                size="small"
                                icon="fa-solid fa-trash-can"
                                :track-for-focus="true"
                                :data-cy="`deleteTag_${slotProps.data.tagId}`"
                                :aria-label="`remove tag ${slotProps.data.tagValue}`">
                  </SkillsButton>
                </ButtonGroup>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numRows }}</span>
            </template>
            <template #empty>
              <table-no-res noResMsg="No Tags." :showResetFilter="true" @resetFilter="clearFilter"/>
            </template>
          </SkillsDataTable>
      </template>
    </Card>
    <no-content2
        v-if="!isInitialLoading && !hasData"
        class="mt-14"
        title="No Tags Yet"
        message="Add custom tags to help users categorize, search, and filter skills."/>
  </div>
  <create-tag-dialog
      v-if="!projConf.isReadOnlyProj && showCreateSkillTagDialog"
      id="addSkillsToBadgeModal"
      v-model="showCreateSkillTagDialog"
      :tag-id-to-edit="editExistingTagId"
      @added-tag="onTagAdded"
  />
  <removal-validation
      v-if="deleteTagInfo.showDialog && !projConf.isReadOnlyProj"
      :item-name="deleteTagInfo.tagName"
      item-type="Tag"
      v-model="deleteTagInfo.showDialog"
      :enable-return-focus="true"
      @do-remove="removeTag"/>
</template>
