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
import { ref, onMounted } from 'vue';
import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService.js';
import SharedSkillsTable from "@/components/skills/crossProjects/SharedSkillsTable.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import {useStorage} from "@vueuse/core";

const props = defineProps(['projectId']);

const loading = ref(true);
const sharedSkills = ref([]);
const sharedSkillsLoadFailed = ref(false);
const possiblePageSizes = [5, 10, 15, 20, 50];
const pageSize = useStorage('sharedWithMeSkillsTable-pageSize', 5)
const totalRecords = ref(0)
let sharedSkillsRequestId = 0
const currentPage = ref(1);
const defaultSort = { sortOrder: -1, sortBy: 'skill.skillId' }
const sortInfo = useStorage('skillsTable-sort-sharedWithMeSkillsTable', defaultSort)
if (!['skill.skillId', 'skill.name', 'projectName'].includes(sortInfo.value?.sortBy)) {
  sortInfo.value = { ...defaultSort }
}

const loadSharedSkills = () => {
  const requestId = ++sharedSkillsRequestId;
  loading.value = true;
  sharedSkillsLoadFailed.value = false;
  const params = {
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy,
  };

  return SkillsShareService.getSharedWithmeSkills(props.projectId, params, false)
      .then((data) => {
        if (requestId !== sharedSkillsRequestId) {
          return;
        }
        sharedSkills.value = data.data;
        totalRecords.value = data.totalCount;
      }).catch(() => {
        if (requestId === sharedSkillsRequestId) {
          sharedSkillsLoadFailed.value = true;
        }
      }).finally(() => {
        if (requestId === sharedSkillsRequestId) {
          loading.value = false;
        }
      });
};

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadSharedSkills();
}

const sortChanged = (sortingInfo) => {
  sortInfo.value.sortBy = sortingInfo.sortField
  sortInfo.value.sortOrder = sortingInfo.sortOrder
  currentPage.value = 1
  loadSharedSkills()
}

onMounted(() => {
  loadSharedSkills();
})
</script>

<template>
  <Card class="mb-4"
        :pt="{ body: { class: 'p-0!' } }"
        data-cy="skillsSharedWithMeCard">
    <template #header>
      <SkillsCardHeader title="Available skills from other projects for use as prerequisites"></SkillsCardHeader>
    </template>
    <template #content>
      <Message v-if="sharedSkillsLoadFailed" severity="error">
        Could not load shared skills. Please try again.
      </Message>
      <div v-if="sharedSkills && sharedSkills.length > 0">
        <shared-skills-table :shared-skills="sharedSkills" :disable-delete="true" :page-size="pageSize" :possible-page-sizes="possiblePageSizes"
                             :total-records="totalRecords" :sort-field="sortInfo.sortBy" :sort-order="sortInfo.sortOrder"
                             @page-changed="pageChanged" @sort-changed="sortChanged"></shared-skills-table>
      </div>
      <div v-else-if="!sharedSkillsLoadFailed">
        <no-content2 title="No Skills Available Yet..." icon="far fa-handshake"
                     class="p-8"
                     message="Coordinate with other projects to share skills with this project."></no-content2>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>
