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
import {computed, onMounted, ref} from "vue";
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

const skillTagState = useSingleSkillTagState()
const tableId = 'skillTagSkillsTable'
const pageSize = useStorage(`${tableId}-pageSize`, 10)
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const route = useRoute()
const skillRouteUtil = useSkillOverviewRouteUtil()
const projConf = useProjConfig()

const loadingProjSkills = ref(true)
const rowsPerPage = [10, 25, 50, 100];
const projectSkills = ref([]);
const nameQuery = ref(null);

onMounted(() => {
  loadProjSkills()
})

const hideManageButton = computed(() => projConf.isReadOnlyProj)
const loading = computed(() => skillTagState.loadingSkillTag || loadingProjSkills.value)

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
}

const toRouteProps = (skill) => {
  const routeProps = skillRouteUtil.toRouteProps(route.params.projectId, skill.subjectId, skill.skillId, SkillType.Skill, skill.groupId)
  return { name: routeProps.name, params: routeProps.params }
}


const loadProjSkills = () => {
  SkillsService.getProjectSkills(route.params.projectId, nameQuery.value, false, true)
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
    console.log(res)
    res = res.filter((item) => item.name.toLowerCase().includes(nameQuery.value.toLowerCase()))
  }
  return res
})

const skillAdded = (newItem) => {
  if (newItem) {

  }
}

const filterSkills = (searchQuery) => {
  nameQuery.value = searchQuery;
}
</script>

<template>
  <div>
    <sub-page-header title="Skills"/>
    <Card :pt="{ 'body': 'p-0! m-0!'}">
      <template #content>
        <skills-spinner :is-loading="loading" class="my-14"/>
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
            :loading="skillTagState.loadingSkillTag"
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

        </SkillsDataTable>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>