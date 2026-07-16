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
import SkillTagChip from '@/skills-display/components/skill/tags/SkillTagChip.vue'

const props = defineProps({
  skill: Object,
})
const emit = defineEmits(['add-tag-filter'])
const skillsDisplayInfo = useSkillsDisplayInfo()
const showBadgesAndTagsRow = computed(() => {
  return ((props.skill.badges && props.skill.badges.length > 0 && !props.badgeId) || (props.skill.tags && props.skill.tags.length > 0))
})

const genLink = (b) => {
  const pageName = b.skillType === 'GlobalBadge' ? 'globalBadgeDetails' : 'badgeDetails'
  return { name: skillsDisplayInfo.getContextSpecificRouteName(pageName), params: { badgeId: b.badgeId } }
}

const hasBadges = computed(() => props.skill.badges && props.skill.badges.length > 0)
const hasTags = computed(()=> props.skill.tags && props.skill.tags.length > 0)
</script>

<template>
  <div v-if="showBadgesAndTagsRow" class="pt-1 items-center">
    <div v-if="hasBadges" class="pr-0" data-cy="skillBadges">
      <i class="fa fa-award text-purple-500" aria-hidden="true"></i> Badges:
      <span v-for="(badge, index) in skill.badges" :data-cy="`skillBadge-${index}`" class="overflow-hidden"
            v-bind:key="badge.badgeId">
              <router-link :to="genLink(badge)" class="skills-theme-primary-color"
                           style="text-decoration:underline;">{{ badge.name }}</router-link>
              <span v-if="index != (skill.badges.length - 1)">, </span>
            </span>
    </div>
    <div v-if="hasTags" data-cy="skillTags" :class="{ 'mt-2': hasBadges }" class="flex gap-2">
      <skill-tag-chip v-for="(tag, index) in skill.tags" :key="tag.tagId"
                      :tag-id="tag.tagId"
                      :tag-value="tag.tagValue"
                      :data-cy="`skillTag-${index}`" />
    </div>
  </div>
</template>

<style scoped>

</style>