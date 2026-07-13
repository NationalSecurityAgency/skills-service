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
import ProjectPageHeader from "@/components/utils/pages/ProjectPageHeader.vue";
import {computed, onMounted, ref} from "vue";
import {useSingleSkillTagState} from "@/stores/UseSingleSkillTagState.js";
import {useRoute} from "vue-router";
import Navigation from "@/components/utils/Navigation.vue";
import CreateTagDialog from "@/components/skills/tags/CreateTagDialog.vue";
import {useProjConfig} from "@/stores/UseProjConfig.js";

const skillTagState = useSingleSkillTagState()
const route = useRoute()
const projConf = useProjConfig()
const tagId = route.params.tagId.toString()
const navItems = computed(() => {
  return [
    { name: 'Tagged Skills', iconClass: 'fa-graduation-cap', page: 'SkillTagSkills' },
    { name: 'Users', iconClass: 'fa-users', page: 'SkillTagUsers' },
  ];
})

onMounted(() => {
  skillTagState.loadSkillTagInfo(route.params.projectId, tagId)
})

const isLoading = computed(() => skillTagState.loadingSkillTag)
const headerOptions = computed(() => {
  const iconClass = 'fa-solid fa-tags'

  return {
    icon: `${iconClass}`,
    title: `TAG: ${skillTagState.skillTag?.tagValue || 'N/A' }`,
    stats: [{
      label: 'Tagged Skills',
      count: skillTagState.skillTag?.skills?.length || 0,
      icon: 'fa-solid fa-graduation-cap'
    }]
  }
})

const showSkillTagDialog = ref(false)
const editExistingTag = () => {
  showSkillTagDialog.value = true
}
const onTagEdited = (newTag) => {
  skillTagState.skillTag.tagValue = newTag.tagValue
}
</script>

<template>
  <div>
    <project-page-header :loading="isLoading" :options="headerOptions">
      <template #subSubTitle>
        <div class="mt-2">
          <SkillsButton :id="`editTag_${tagId}`"
                        @click="editExistingTag()"
                        label="Edit Tag"
                        icon="fa-solid fa-edit"
                        size="small"
                        severity="info"
                        :track-for-focus="true"
                        data-cy="editTag"
                        :aria-label="`edit tag ${skillTagState.skillTag?.tagValue}`">
          </SkillsButton>
        </div>
      </template>
    </project-page-header>

    <navigation :nav-items="navItems" />

    <create-tag-dialog
        v-if="!projConf.isReadOnlyProj && showSkillTagDialog"
        id="addSkillsToBadgeModal"
        v-model="showSkillTagDialog"
        :tag-id-to-edit="tagId"
        @added-tag="onTagEdited"
    />
  </div>
</template>

<style scoped>

</style>