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

import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import {useSingleSkillTagState} from "@/stores/UseSingleSkillTagState.js";
import {computed, nextTick, onMounted, ref} from "vue";
import {useStorage} from "@vueuse/core";
import Column from "primevue/column";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import SkillType from "@/common-components/utilities/SkillType.js";
import {useRoute} from "vue-router";
import {useSkillOverviewRouteUtil} from "@/components/skills/UseSkillOverviewRouteUtil.js";
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import {useProjConfig} from "@/stores/UseProjConfig.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import NoContent2 from "@/components/utils/NoContent2.vue";

const skillTagState = useSingleSkillTagState()
const tableId = 'skillTagSkillsTable'
const pageSize = useStorage(`${tableId}-pageSize`, 25)
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const route = useRoute()
const skillRouteUtil = useSkillOverviewRouteUtil()
const projConf = useProjConfig()
const dialogMessages = useDialogMessages()

const loadingProjSkills = ref(true)
const addingSkillToTag = ref(false)
const rowsPerPage = [10, 25, 50, 100];
const projectSkills = ref([]);
const nameQuery = ref(null);

onMounted(() => {
  loadProjSkills()
})

const hideManageButton = computed(() => projConf.isReadOnlyProj)
const loading = computed(() => skillTagState.loadingSkillTag || loadingProjSkills.value || addingSkillToTag.value)

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
}

const toRouteProps = (skill) => {
  const routeProps = skillRouteUtil.toRouteProps(route.params.projectId, skill.subjectId, skill.skillId, SkillType.Skill, skill.groupId)
  return { name: routeProps.name, params: routeProps.params }
}


const loadProjSkills = () => {
  return SkillsService.getProjectSkills(route.params.projectId, nameQuery.value, false, true)
      .then((loadedSkills) => {
        projectSkills.value = loadedSkills
      }).finally(() => {
        loadingProjSkills.value = false;
      })
};

const availableSkills = computed(() => {
  const tagSkillIds = skillTagState.skills?.map((item) => item.skillId) || [];
  let res = projectSkills.value.filter((item) => !tagSkillIds.includes(item.skillId));
  if (nameQuery.value?.length > 0) {
    res = res.filter((item) => item.name.toLowerCase().includes(nameQuery.value.toLowerCase()))
  }
  return res
})

const skillAdded = (newItem) => {
  addingSkillToTag.value = true
  const projectId = route.params.projectId
  const {tagId, tagValue} = skillTagState.skillTag
  return SkillsService.addTagToSkills(projectId, [newItem.skillId], tagId, tagValue)
      .then(() => {
        return skillTagState.loadSkillTagInfo(projectId, tagId)
      }).finally(() => {
        addingSkillToTag.value = false;
      }).then(() => {
        focusOnSkillsSelector()
      })
}

const filterSkills = (searchQuery) => {
  nameQuery.value = searchQuery;
}

const removeSkill = (skill) => {
  const {tagValue} = skillTagState.skillTag
  const msg = `Are you sure you want to remove Skill "${skill.skillName}" from Tag "${tagValue}"?`;
  dialogMessages.msgConfirm({
    target: skill.currentTarget,
    message: msg,
    header: 'WARNING: Remove Skill from Tag',
    acceptLabel: 'YES, Remove It!',
    rejectLabel: 'Cancel',
    disableAutoFocus: false,
    accept: () => {
      doRemoveSkill(skill);
    },
  });
}
const doRemoveSkill = (skill) => {
  const {tagId} = skillTagState.skillTag
  return SkillsService.deleteTagForSkills(route.params.projectId, [skill.skillId], tagId)
      .then(() => {
            return skillTagState.loadSkillTagInfo(route.params.projectId, tagId)
          }
      ).finally(() => {
        focusOnSkillsSelector()
      })
}

const skillsSelector = ref(null)
const focusOnSkillsSelector = () => {
  nextTick(() => {
    skillsSelector.value?.focus()
  })
}
</script>

<template>
  <div>
    <sub-page-header title="Tagged Skills"/>
    <Card :pt="{ 'body': 'p-0! m-0!'}">
      <template #content>
        <skills-spinner v-if="loading" :is-loading="loading" class="my-14"/>

        <div v-if="!loading" class="flex flex-col gap-2">
          <div class="p-3">
            <skills-selector
                v-if="!projConf.isReadOnlyProj"
                :options="availableSkills"
                ref="skillsSelector"
                v-on:added="skillAdded"
                @search-change="filterSkills"
                :internal-search="false"
                :showClear="false"/>
          </div>

          <SkillsDataTable
              v-if="skillTagState.hasSkills"
              :tableStoredStateId="tableId"
              aria-label="Tag Skills"
              :value="skillTagState.skills"
              :loading="loading"
              paginator
              :rows="pageSize"
              :totalRecords="skillTagState.numSkills"
              :rowsPerPageOptions="rowsPerPage"
              @page="pageChanged"
              data-cy="skillTagSkillsTable">
            <Column header="Skill" field="skillName" sortable :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-graduation-cap mr-1" :class="colors.getTextClass(0)" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <router-link :to="toRouteProps(slotProps.data)"
                             class="underline"
                             :data-cy="`manage_${slotProps.data.skillId}`">
                  {{ slotProps.data.skillName }}
                </router-link>
              </template>
            </Column>
            <Column header="Subject" field="subjectName" sortable :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-tag mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <router-link v-if="!hideManageButton" :id="slotProps.data.subjectId" :to="{ name:'SubjectSkills',
                  params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId }}"
                             class="btn btn-sm btn-outline-hc ml-2"
                             :data-cy="`manage_${slotProps.data.subjectId}`">
                  {{ slotProps.data.subjectName }}
                </router-link>
                <div v-else>{{ slotProps.data.subjectName }}</div>
              </template>
            </Column>
            <Column header="Group" field="groupName" sortable :class="{'flex': responsive.md.value }">
              <template #header>
                <i class="fa-solid fa-layer-group mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
              </template>
              <template #body="slotProps">
                <div>{{ slotProps.data.groupName }}</div>
              </template>
            </Column>
            <Column v-if="!hideManageButton" header="Delete" :class="{'flex': responsive.md.value }">
              <template #body="slotProps">
                <SkillsButton :id="`removeSkill_${slotProps.data.skillId}`"
                              @click="removeSkill(slotProps.data)"
                              size="small"
                              icon="fa-solid fa-trash-can"
                              label="Remove"
                              :track-for-focus="true"
                              :data-cy="`deleteSkill_${slotProps.data.skillId}`"
                              :aria-label="`remove skill ${slotProps.data.skillId} from the tag`">
                </SkillsButton>
              </template>
            </Column>

          </SkillsDataTable>
          <no-content2
              v-if="!loading && !skillTagState.hasSkills"
              class="my-10"
              title="No Skills Added Yet..."
              message="Please use drop-down above to start adding skills to this Tag!"/>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>