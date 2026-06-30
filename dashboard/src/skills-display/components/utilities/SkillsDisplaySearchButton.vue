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
import SkillType from "@/common-components/utilities/SkillType.js";
import NavToSearchButton from "@/common-components/search/NavToSearchButton.vue";
import {useSkillsDisplayInfo} from "@/skills-display/UseSkillsDisplayInfo.js";
import {useSkillsDisplayService} from "@/skills-display/services/UseSkillsDisplayService.js";

const props = defineProps({
  showLabel: {
    type: Boolean,
    default: true
  },
  keyboardShortcutEnabled: {
    type: Boolean,
    default: true
  }
});

const skillsDisplayService = useSkillsDisplayService()
const skillDisplayInfo = useSkillsDisplayInfo()

const navToSkill = (skill) => {
  const { skillType } = skill
  if (SkillType.isSubject(skillType)) {
    skillDisplayInfo.routerPush(
        'SubjectDetailsPage',
        {
          subjectId: skill.skillId
        })
  } else if (SkillType.isBadge(skillType)) {
    skillDisplayInfo.routerPush(
        'badgeDetails',
        {
          badgeId: skill.skillId
        })
  } else if (SkillType.isSkillsGroup(skillType)) {
    skillDisplayInfo.routerPush(
        'skillsGroupDetails',
        {
          subjectId: skill.subjectId,
          groupId: skill.skillId
        })
  } else {
    const pageName = skill.skillsGroupId ? 'skillDetailsUnderGroup' : 'skillDetails'
    skillDisplayInfo.routerPush(
        pageName,
        {
          subjectId: skill.subjectId,
          skillId: skill.skillId,
          groupId: skill.skillsGroupId,
        })
  }
}
</script>

<template>
  <nav-to-search-button
      :nav-to-skill-fn="navToSkill"
      :load-proj-pages-info-fn="skillsDisplayService.getAllProjectSkillsSubjectsAndBadges"
      :show-label="showLabel"
      :keyboard-shortcut-enabled="keyboardShortcutEnabled"
  />

</template>

<style scoped>

</style>