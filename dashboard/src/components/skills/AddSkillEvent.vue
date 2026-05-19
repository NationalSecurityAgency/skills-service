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
import {useRoute} from 'vue-router'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import Message from 'primevue/message'

const props = defineProps({
  projectId: String,
})

const route = useRoute()

</script>

<template>
  <div>
    <SubPageHeader title="Add Skill Events"/>
    <Message :closable="false" class="p-2">
      This page has been replaced by the ability to add multiple skills to multiple users at once from the Skills table on the Subject page.
      This feature can be used from the skills table by selecting skills and then selecting "Report Skills for Users" from the Action menu.

      <div class="mt-6 text-center">
        <router-link :to="`/administrator/projects/${route.params.projectId}/subjects/${route.params.subjectId}`">
          <SkillsButton class="p-2" data-cy="returnToSubjects" label="Go to the Subject Page"></SkillsButton>
        </router-link>
      </div>
    </Message>
  </div>
</template>

<style scoped></style>
