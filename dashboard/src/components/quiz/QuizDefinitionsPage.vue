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
import { ref } from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import QuizDefinitions from '@/components/quiz/QuizDefinitions.vue';

const quizDefinitionsRef = ref(null)
const quizDefPageSubPageHeader = ref(null)
function openNewTestModal() {
  quizDefinitionsRef.value.showUpdateModal({
    name: '',
    testId: '',
    type: 'Quiz',
    description: '',
  }, false);
}
</script>

<template>
  <div>
    <SubPageHeader ref="quizDefPageSubPageHeader"
                   size="small"
                   title="Quizzes And Surveys"
                   action="Quiz/Survey"
                   aria-label="Create New Quiz or Survey"
                   :title-level="1"
                   @add-action="openNewTestModal">
    </SubPageHeader>
    <Card :pt="{ body: { class: '!p-0' } }">
      <template #content>
        <QuizDefinitions ref="quizDefinitionsRef"/>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>