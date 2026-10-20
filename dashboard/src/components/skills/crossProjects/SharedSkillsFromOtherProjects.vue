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
const possiblePageSizes = [5, 10, 15, 20, 50];
const pageSize = useStorage('sharedSkillsTable-pageSize', 5)
const totalRecords = ref(0)
const currentPage = ref(1);
const sortField = ref('skill.skillId')
const sortOrder = ref(0)

const loadSharedSkills = () => {
  loading.value = true;
  const params = {
    limit: pageSize.value,
    ascending: sortOrder.value === 1,
    page: currentPage.value,
    orderBy: sortField.value,
  };

  SkillsShareService.getSharedWithmeSkills(props.projectId, params)
      .then((data) => {
        sharedSkills.value = data.data;
        totalRecords.value = data.totalCount;
        loading.value = false;
      });
};

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadSharedSkills();
}

const sortChanged = (sortingInfo) => {
  sortField.value = sortingInfo.sortField
  sortOrder.value = sortingInfo.sortOrder
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
      <div v-if="sharedSkills && sharedSkills.length > 0">
        <shared-skills-table :shared-skills="sharedSkills" :disable-delete="true" :page-size="pageSize" :possible-page-sizes="possiblePageSizes"
                             :total-records="totalRecords" @page-changed="pageChanged" @sort-changed="sortChanged"></shared-skills-table>
      </div>
      <div v-else>
        <no-content2 title="No Skills Available Yet..." icon="far fa-handshake"
                     class="p-8"
                     message="Coordinate with other projects to share skills with this project."></no-content2>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>