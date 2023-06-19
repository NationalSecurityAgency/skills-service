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
  <div>
      <v-select
          :options="searchRes"
          label="skillName"
          placeholder="Search for a skill across subjects..."
          v-on:search="queryChanged"
          v-on:option:selected="navToSkill"
          data-cy="searchSkillsAcrossSubjects">
        <template #option="option">
          <div class="py-1 skill-res-row" :data-cy="`searchRes-${option.skillId}`">
            <div data-cy="subjectName" aria-hidden="true">
              <span class="font-italic ">Subject:</span> <span class="text-info skills-theme-primary-color alt-color-handle-hover">{{ option.subjectName }}</span>
            </div>
            <div class="row">
              <div class="col h4" data-cy="skillName"
                :aria-label="`Selected ${option.skillName} skill from ${option.subjectName} subject. You have earned ${option.userCurrentPoints} points out of ${option.totalPoints} for this skill. Click to navigate to the skill. Type to search for a skill across all subjects.`">
                <i class="fas fa-graduation-cap text-info skills-theme-primary-color alt-color-handle-hover" aria-hidden="true" />  <span v-if="option.skillNameHtml" v-html="option.skillNameHtml"></span><span v-else>{{ option.skillName }}</span>
              </div>
              <div class="col-auto skills-theme-primary-color alt-color-handle-hover" data-cy="points" :class="{'text-success': option.userAchieved}" aria-hidden="true">
                <i option v-if="option.userAchieved" class="fas fa-check" aria-hidden=""/> {{ option.userCurrentPoints }} / {{ option.totalPoints }} <span class="font-italic">Points</span>
              </div>
            </div>
          </div>
        </template>
        <template #no-options>
          <div class="pt-2 pl-3 text-left">
            <span class="h5">No skills found. Consider changing the search query...</span>
          </div>
        </template>
      </v-select>
  </div>
</template>

<script>
  import vSelect from 'vue-select';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import debounce from 'lodash/debounce';

  export default {
    name: 'SearchAllProjectSkills',
    components: { vSelect },
    mixins: [NavigationErrorMixin],
    data() {
      return {
        query: '',
        searchRes: [],
      };
    },
    mounted() {
      this.search();
    },
    methods: {
      search() {
        return UserSkillsService.searchSkills(this.query)
          .then((res) => {
            let results = res.data;
            if (results && this.query && this.query.trim().length > 0) {
              results = results.map((item) => {
                const skillNameHtml = StringHighlighter.highlight(item.skillName, this.query);
                return ({ ...item, skillNameHtml });
              });
            }
            this.searchRes = results;
            if (results && results.length > 0) {
              const firstRes = results[0];
              // eslint-disable-next-line max-len
              this.$nextTick(() => this.$announcer.polite(`Showing ${results.length} skills for ${this.query ? this.query : 'an empty'} search string. Selected ${firstRes.skillName} skill from ${firstRes.subjectName} subject. You have earned ${firstRes.userCurrentPoints} points out of ${firstRes.totalPoints} for this skill. Click to navigate to the skill.`));
            } else {
              this.$nextTick(() => this.$announcer.assertive(`No skills found for ${this.query} search string. Consider changing the search query.`));
            }
          });
      },
      queryChanged(query, loading) {
        this.query = query;
        loading(true);
        this.searchWithDebounce(loading);
      },
      searchWithDebounce: debounce(function debouncedValidate(loading) {
        this.search().then(() => loading(false));
      }, 400),
      navToSkill(skill) {
        this.handlePush({
          name: 'skillDetails',
          params: {
            subjectId: skill.subjectId,
            skillId: skill.skillId,
          },
        });
      },
    },
  };
</script>

<style scoped>
.vs__dropdown-option--highlight .alt-color-handle-hover {
  color: white !important;
}
</style>
