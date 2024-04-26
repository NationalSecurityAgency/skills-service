<script setup>
import { computed } from 'vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const props = defineProps({
  skill: Object,
  badgeId: {
    type: String,
    required: false
  },
  enableToAddTag: {
    type: Boolean,
    default: false
  }
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
const addTagFilter = (tag) => {
  emit('add-tag-filter', tag)
}
</script>

<template>
  <div v-if="showBadgesAndTagsRow" class="row" style="padding-top:8px;">
    <div v-if="skill.badges && skill.badges.length > 0 && !badgeId" class="col-auto pr-0" style="font-size: 0.9rem"
         data-cy="skillBadges">
      <i class="fa fa-award text-purple-500" aria-hidden="true"></i> Badges:
      <span v-for="(badge, index) in skill.badges" :data-cy="`skillBadge-${index}`" class="overflow-hidden"
            v-bind:key="badge.badgeId">
              <router-link :to="genLink(badge)" class="skills-theme-primary-color"
                           style="text-decoration:underline;">{{ badge.name }}</router-link>
              <span v-if="index != (skill.badges.length - 1)">, </span>
            </span>
    </div>
    <div v-if="skill.tags && skill.tags.length > 0" class="col" style="font-size: 0.9rem" data-cy="skillTags">
            <span v-for="(tag, index) in skill.tags" :data-cy="`skillTag-${index}`" class="overflow-hidden pr-1"
                  v-bind:key="tag.tagId">
              <Tag class="py-1 px-2" severity="info" :href="enableToAddTag ? '#' : ''" @click="addTagFilter(tag)">
                  <span>{{ tag.tagValue }} <i class="fas fa-search-plus"></i></span>
                </Tag>
            </span>
    </div>
  </div>
</template>

<style scoped>

</style>