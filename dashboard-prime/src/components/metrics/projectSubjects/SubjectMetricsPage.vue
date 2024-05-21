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
