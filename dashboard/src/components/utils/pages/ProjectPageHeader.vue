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
import PageHeader from "@/components/utils/pages/PageHeader.vue";
import SearchButton from "@/common-components/components/SearchButton.vue";
import SkillType from "@/common-components/utilities/SkillType.js";
import {useRoute, useRouter} from "vue-router";
import SkillsService from "@/components/skills/SkillsService.js";

defineProps(['loading', 'options'])

const router = useRouter()
const route = useRoute()

const projectId = route.params.projectId

const navToSkill = (skill) => {
  const { skillType } = skill
  if (SkillType.isSubject(skillType)) {
    router.push({ name: 'SubjectSkills', params:{ projectId, subjectId: skill.skillId}})
  } else if (SkillType.isBadge(skillType)) {
    router.push({ name: 'BadgeSkills', params:{ projectId, badgeId: skill.skillId}})
  } else if (SkillType.isSkillsGroup(skillType)) {
    router.push({ name: 'GroupSkills', params: { projectId, subjectId: skill.subjectId, groupId: skill.skillId }})
  } else {
    const name = skill.skillsGroupId ? 'GroupSkillOverview' : 'SingleSkillOverview'
    const params = {
      projectId,
      subjectId: skill.subjectId,
      skillId: skill.skillId,
      groupId: skill.skillsGroupId,
    }
    router.push({name, params})
  }
}

const loadProjPagesInfo = () => SkillsService.getProjectNavigableItems(projectId)
</script>

<template>
  <page-header :loading="loading" :options="options">
    <template #top-right>
      <div class="text-right">
        <search-button :nav-to-skill-fn="navToSkill" :load-proj-pages-info-fn="loadProjPagesInfo"/>
      </div>
    </template>
  </page-header>

</template>

<style scoped>

</style>