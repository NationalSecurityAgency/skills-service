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
import UserCountsBySubjectMetric from "@/components/metrics/projectSubjects/UserCountsBySubjectMetric.vue";
import SubjectLevelsOverTime from "@/components/metrics/projectSubjects/SubjectLevelsOverTime.vue";
</script>

<template>
  <div>
    <user-counts-by-subject-metric />
    <subject-levels-over-time class="my-3"/>
  </div>
</template>

<style scoped></style>
