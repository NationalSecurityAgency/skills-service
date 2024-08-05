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