/*
Copyright 2020 SkillTree

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
<template>
  <router-link :data-cy="`manageSkillLink_${skill.skillId}`"
               tag="a"
               :to="{ name:'SkillOverview', params: { projectId: skill.projectId, subjectId: subjectId, skillId: skill.skillId }}"
               :aria-label="`Manage skill ${skill.name}  via link`">
    <div class="h5 d-inline-block"><show-more :text="skill.nameHtml ? skill.nameHtml : skill.name" :limit="45" :contains-html="!!skill.nameHtml" /></div>
  </router-link>
</template>

<script>
  import ShowMore from '@/components/skills/selfReport/ShowMore';

  export default {
    name: 'SkillNameRouterLink',
    components: { ShowMore },
    props: {
      skill: Object,
      subjectId: String,
    },
  };
</script>

<style scoped>

</style>
