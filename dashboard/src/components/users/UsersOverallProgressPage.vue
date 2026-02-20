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

import {computed, ref} from "vue";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";

const hasData = computed(() => true)
</script>

<template>
<div>
  <SubPageHeader title="Users Progress" :title-level="1">
  </SubPageHeader>
  <Card v-if="hasData">
    <template #content>
      Users table here
    </template>
  </Card>
  <no-content2 v-if="!hasData" class="mt-6"
               title="No User Data Available" message="Cross-project user activity and progress data will display here once users start completing skills and taking quizzes across your administered projects"></no-content2>
</div>
</template>

<style scoped>

</style>