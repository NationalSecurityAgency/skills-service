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
  <div class="row mb-2" style="height: 3rem;">
    <div class="col">
      <skills-selector2 :options="availableSkills"
                      class="search-and-nav border rounded"
                      v-on:search-change="searchChanged"
                      v-on:added="navToSkill"
                      :is-loading="loading"
                      placeholder="Search and Navigate directly to a skill"
                      placeholder-icon="fas fa-search"
                      select-label="Click to navigate"
                      :onlySingleSelectedValue="true"
                      :internal-search="false"
                      :empty-without-search="true"/>
    </div>
  </div>
</template>

<script>
  import SkillsService from '../skills/SkillsService';
  import SkillsSelector2 from '../skills/SkillsSelector2';

  export default {
    name: 'JumpToSkill',
    components: { SkillsSelector2 },
    data() {
      return {
        loading: true,
        availableSkills: [],
        search: '',
      };
    },
    methods: {
      searchChanged(query) {
        this.search = query;
        if (this.search && this.search.length > 0) {
          this.loading = true;
          SkillsService.getProjectSkills(this.$route.params.projectId, this.search).then((result) => {
            this.availableSkills = result;
            this.loading = false;
          });
        } else {
          this.availableSkills = [];
        }
      },
      navToSkill(selectedItem) {
        this.$router.push({
          name: 'SkillOverview',
          params: { projectId: selectedItem.projectId, subjectId: selectedItem.subjectId, skillId: selectedItem.skillId },
        });
      },
    },
  };
</script>

<style scoped>
.search-and-nav {
  border-color: #d9d9d9 !important;
}
</style>
