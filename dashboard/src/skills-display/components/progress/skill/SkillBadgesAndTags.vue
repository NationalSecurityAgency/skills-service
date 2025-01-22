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
import { computed } from 'vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useRoute } from 'vue-router'

const props = defineProps({
  skill: Object,
  enableToAddTag: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['add-tag-filter'])
const skillsDisplayInfo = useSkillsDisplayInfo()
const route = useRoute()
const showBadgesAndTagsRow = computed(() => {
  return ((props.skill.badges && props.skill.badges.length > 0 && !props.badgeId) || (props.skill.tags && props.skill.tags.length > 0))
})

const genLink = (b) => {
  const pageName = b.skillType === 'GlobalBadge' ? 'globalBadgeDetails' : 'badgeDetails'
  return { name: skillsDisplayInfo.getContextSpecificRouteName(pageName), params: { badgeId: b.badgeId } }
}
const addTagFilter = (tag) => {
  emit('add-tag-filter', tag)
}

const hasBadges = computed(() => props.skill.badges && props.skill.badges.length > 0 && skillsDisplayInfo.isSubjectPage.value)
const hasTags = computed(()=> props.skill.tags && props.skill.tags.length > 0)
</script>

<template>
  <div v-if="showBadgesAndTagsRow" class="pt-1">
    <div v-if="hasBadges" class="pr-0" data-cy="skillBadges">
      <i class="fa fa-award text-purple-500" aria-hidden="true"></i> Badges:
      <span v-for="(badge, index) in skill.badges" :data-cy="`skillBadge-${index}`" class="overflow-hidden"
            v-bind:key="badge.badgeId">
              <router-link :to="genLink(badge)" class="skills-theme-primary-color"
                           style="text-decoration:underline;">{{ badge.name }}</router-link>
              <span v-if="index != (skill.badges.length - 1)">, </span>
            </span>
    </div>
    <div v-if="hasTags" data-cy="skillTags" :class="{ 'mt-2': hasBadges }">
      <Chip v-for="(tag, index) in skill.tags"
            :data-cy="`skillTag-${index}`"
            v-bind:key="tag.tagId"
            class="py-0 pl-0 pr-4 mr-2">
        <span class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center"><i
          class="fas fa-tag" /></span>
        <span class="ml-2 font-medium">{{ tag.tagValue }}</span>
      <SkillsButton
        icon="fas fa-search-plus"
        size="small"
        class="py-1 pl-0 pr-1 text-sm ml-1"
        severity="secondary"
        data-cy="addTagBtn"
        text
        @click="addTagFilter(tag)" />
      </Chip>
    </div>
  </div>
</template>

<style scoped>

</style>