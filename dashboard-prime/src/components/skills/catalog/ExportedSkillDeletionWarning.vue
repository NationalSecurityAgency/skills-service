<script setup>
defineProps({
  skillName: String,
  loadedStats: Object
})
</script>

<template>
  <div>
    <p>
      This will <span class="text-primary font-weight-bold">PERMANENTLY</span> remove <span
      class="text-primary font-weight-bold">[{{ skillName }}]</span> Skill from the catalog.
      This skill is currently imported by
      <Tag severity="info">{{ loadedStats.users.length }}</Tag>
      project{{ loadedStats.users.length === 1 ? '' : 's' }}.
    </p>
    <p>
      <span v-if="loadedStats.users.length > 0">This action <b>CANNOT</b> be undone and will permanently remove the skill from those projects including their achievements.</span>
      Please proceed with care.
    </p>
  </div>
</template>

<style scoped>

</style>