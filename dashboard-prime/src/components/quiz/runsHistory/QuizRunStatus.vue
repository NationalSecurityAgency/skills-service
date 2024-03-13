<script setup>

const props = defineProps({
  quizType: String,
  status: String,
})
</script>

<template>
  <div>
    <div v-if="status === 'FAILED'" class="text-danger">
      <i class="far fa-times-circle" aria-hidden="true"></i> Failed
    </div>
    <div v-if="status === 'PASSED'" class="text-success">
      <i class="fas fa-check-double" aria-hidden="true"></i> <span v-if="quizType === 'Survey'">Completed</span><span v-else>Passed</span>
    </div>
    <div v-if="status === 'INPROGRESS'" class="text-info">
      <i class="fas fa-running" aria-hidden="true"></i> In Progress
    </div>
  </div>
</template>

<style scoped>

</style>