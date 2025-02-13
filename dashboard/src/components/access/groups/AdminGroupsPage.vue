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
import {ref} from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import GroupDefinitions from '@/components/access/groups/AdminGroupDefinitions.vue';

const groupDefinitionsRef = ref(null)
const adminGroupsPageSubPageHeader = ref(null)
function openNewGroupModal() {
  groupDefinitionsRef.value.showUpdateModal({
    name: '',
    testId: '',
    type: 'Group',
    description: '',
  }, false);
}
</script>

<template>
  <div>
    <SubPageHeader ref="adminGroupsPageSubPageHeader"
                   size="small"
                   title="Admin Groups"
                   action="Admin Group"
                   aria-label="Create a new Group of Administrators"
                   :title-level="1"
                   @add-action="openNewGroupModal">
    </SubPageHeader>
    <Card :pt="{ body: { class: '!p-0' } }">
      <template #content>
        <GroupDefinitions ref="groupDefinitionsRef"/>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>