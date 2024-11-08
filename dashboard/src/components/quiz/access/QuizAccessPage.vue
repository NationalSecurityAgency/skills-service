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

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import RoleManager from '@/components/access/RoleManager.vue';


const route = useRoute()
const quizId = computed(() => {
  return route.params.quizId
});
</script>

<template>
  <div>
    <SubPageHeader title="Access" />

    <role-manager id="add-group-admin-access-user"
                  :quiz-id="quizId"
                  title=""
                  role-description="Quiz Admin"
                  data-cy="existingUserInput"
                  :roles="['ROLE_QUIZ_ADMIN']">
    </role-manager>
  </div>
</template>

<style scoped>

</style>