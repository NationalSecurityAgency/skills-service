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
import { ref, onMounted } from 'vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import ReusedTag from '@/components/utils/misc/ReusedTag.vue'

const shouldShow = defineModel()
const emit = defineEmits(['do-remove'])
const props = defineProps({
  skill: {
    type: Object,
    required: true
  }
})

const loading = ref(true)
const loadedStats = ref()
onMounted(() => {
  CatalogService.getExportedStats(props.skill.projectId, props.skill.skillId)
    .then((res) => {
      loadedStats.value = res
    })
    .finally(() => {
      loading.value = false
    })
})

const doRemove = (event) => {
  emit('do-remove', event)
}
</script>

<template>
  <removal-validation
    v-model="shouldShow"
    :loading="loading"
    item-type="skill"
    :item-name="skill.name"
    @do-remove="doRemove">
    <div v-if="skill.reusedSkill">The skill is
      <reused-tag />
      and this action will <b>only</b> remove the reused skill, and not the original!
    </div>
    <div v-if="skill.isSkillType">
      Delete Action <b class="text-danger">CANNOT</b> be undone and permanently removes users'
      performed skills and any dependency associations.
    </div>
    <div v-if="skill.isGroupType">
      Delete Action <b class="text-danger">CANNOT</b> be undone and will permanently remove all of
      the group's skills. All the associated users' performed skills and any dependency
      associations will also be removed.
    </div>
    <div v-if="loadedStats.isExported" class="alert alert-info mt-3">
      <div>
        <p>
          This will <span class="font-bold">PERMANENTLY</span> remove <span
          class="text-primary font-weight-bold">[{{ skill.name }}]</span> Skill from the catalog.
          This skill is currently imported by
          <Tag severity="info">{{ loadedStats.users.length }}</Tag>
          project{{ loadedStats.users.length === 1 ? '' : 's' }}.
        </p>
        <p>
          <span v-if="loadedStats.users.length > 0">This action <b>CANNOT</b> be undone and will permanently remove the skill from those projects including their achievements.</span>
          Please proceed with care.
        </p>
      </div>
    </div>
    <div v-if="loadedStats.isReusedLocally" class="alert alert-info mt-3">
      Please note that the skill is currently
      <reused-tag />
      in this project.
      Deleting this skill will also remove its reused copies.
    </div>
  </removal-validation>
</template>

<style scoped>

</style>