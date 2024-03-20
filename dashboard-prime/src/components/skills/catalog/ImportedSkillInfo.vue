<script setup>
const props = defineProps({
  skill: Object
})
</script>

<template>
<div :data-cy="`importSkillInfo-${skill.projectId}_${skill.skillId}`"  class="ml-5">
  <div v-if="skill.importedProjectCount > 0">
  </div>
  <div v-else>
    <Message :closable="false">This skill has not been imported by any other projects yet...</Message>
  </div>
</div>
</template>

<style scoped>

</style>