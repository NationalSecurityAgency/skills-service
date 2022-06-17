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
<span>
  <router-link v-if="!loading"
               tag="a"
               :to="{ name:'SkillOverview', params: { projectId: this.projectId, subjectId: this.skill.subjectId, skillId:  this.skill.skillId }}"
               :aria-label="`Navigate to skill ${this.skill.name}  via link`">
      <div class="d-inline-block" style="text-decoration: underline">
        <span v-if="linkLabel">{{ linkLabel }}</span>
        <show-more v-else :text="this.skill.name" :limit="45" :contains-html="false"/>
      </div>
  </router-link>
</span>
</template>

<script>
  import SkillsService from '@/components/skills/SkillsService';
  import ShowMore from '@/components/skills/selfReport/ShowMore';

  export default {
    name: 'LinkToSkillPage',
    components: { ShowMore },
    props: {
      projectId: String,
      skillId: String,
      linkLabel: {
        type: String,
        default: null,
      },
    },
    data() {
      return {
        loading: true,
        skill: null,
      };
    },
    mounted() {
      SkillsService.getSkillInfo(this.projectId, this.skillId)
        .then((res) => {
          this.skill = res;
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>
