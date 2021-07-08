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
  <div id="contact-users-panel">
    <sub-page-header title="Contact Users" />

    <b-card body-class="p-0">
      <div class="row p-3 m-2">
        <div class="col-12 col-md-3 border-md-right">
          <b-form-group label="Type:" label-for="filter-type" label-class="text-muted">
            <b-form-radio-group id="type-radio-group" v-model="currentFilterType" name="filter-type-options" stacked>
              <b-form-radio value="project">Project</b-form-radio>
              <b-form-radio value="badge">Badge</b-form-radio>
              <b-form-radio value="subject">Subject</b-form-radio>
              <b-form-radio value="skill">Skill</b-form-radio>
            </b-form-radio-group>
          </b-form-group>
        </div>
        <div class="col-9">
          <div class="row p-3">
              <b-form-group id="levels-input-group" label="Minimum Level (Project & Subject Only):" label-for="input-3" label-class="text-muted">
                <b-form-select id="input-3" v-model="levels.selected" :options="levels.available"
                               required data-cy="emailUsers-levelsInput"
                               :disabled="levelsDisabled" />
              </b-form-group>
            </div>
            <b-form-group label="Name (Subject, Skill and Badge Only):" label-for="name-filter" label-class="text-muted">
              <b-overlay :show="loading.skills || loading.badges || loading.subjects" rounded="sm" opacity="0.5"
                         spinner-variant="info" spinner-type="grow" spinner-small>
                <template v-if="currentFilterType && currentFilterType !== 'project'">
                  <vue-typeahead-bootstrap id="name-filter" v-model="selectedItemQuery"
                                           @hit="selectedItem = $event"
                                           data-cy="emailUsers-nameInput"
                                           :showOnFocus="true"
                                           :max-matches="15"
                                           :disable-sort="false"
                                           :placeholder="`Search ${formattedType}`"
                                           :required="currentFilterType !== 'project'"
                                           :serializer="serializer"
                                            :data="ids"/>

                 <b-form-group label="Achieved" label-for="achieved-button" label-class="text-muted" v-show="currentFilterType && currentFilterType==='skill'" class="mt-4">
                    <b-form-checkbox v-model="skills.achieved"
                                     name="achieved-button"
                                     aria-labelledby="productionModeEnabledLabel"
                                     data-cy="productionModeEnabledSwitch"
                                     switch>
                      {{ skills.achieved ? 'Achieved' : 'Not Achieved' }}
                    </b-form-checkbox>
                  </b-form-group>
                </template>
                <b-form-input v-else disabled/>
              </b-overlay>

            </b-form-group>
          </div>
      </div>
      <div class="row p-3 m-3">
        <b-button variant="outline-primary" @click="addCriteria" data-cy="emailUsers-addBtn" :disabled="isAddDisabled" class="float-right"><i class="fas fa-plus-circle"/> Add</b-button>
      </div>

      <div class="container-fluid p-3 m-3">
        <h1 class="h5 text-uppercase">Users To Contact</h1>
        <b-badge v-for="(tag) of tags" :key="tag.display" variant="info" class="m-2 text-break" style="max-width: 85%;">
          {{tag.display}} <b-button @click="deleteCriteria(tag)"
                                    variant="outline-info" size="sm" class="text-warning"
                                    :aria-label="`Remove contact user criteria ${tag.display}`"
                                    data-cy="contactUserCriteria-removeBtn"><i class="fa fa-trash" /></b-button>
        </b-badge>
      </div>
      <div class="row p-3 m-3 w-100">
        <b-form-group id="subject-line-input-group" label="Subject Line" label-for="subject-line-input" label-class="text-muted">
          <b-input v-model="subject" id="subject-line-input" />
        </b-form-group>
      </div>
      <div class="row p-3 m-3">
        <b-form-group id="body-input-group" label="Email Body" label-for="body-input" label-class="text-muted">
          <markdown-editor v-model="body" />
        </b-form-group>
      </div>
    </b-card>
  </div>

</template>

<script>
  // import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import VueTypeaheadBootstrap from 'vue-typeahead-bootstrap';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  // import InlineHelp from '../utils/InlineHelp';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import SkillsService from '../skills/SkillsService';
  import LevelService from '../levels/LevelService';
  import BadgeService from '../badges/BadgesService';
  import SubjectService from '../subjects/SubjectsService';

  const nameSort = (one, two) => {
    const nameOne = one.name.toUpperCase();
    const nameTwo = two.name.toUpperCase();
    if (nameOne < nameTwo) {
      return -1;
    }
    if (nameOne > nameTwo) {
      return 1;
    }
    return 0;
  };

  export default {
    name: 'EmailUsers',
    components: {
      // SkillsSpinner,
      SubPageHeader,
      // InlineHelp,
      MarkdownEditor,
      VueTypeaheadBootstrap,
    },
    data() {
      return {
        tags: [],
        subject: '',
        body: '',
        selectedItemQuery: '',
        selectedItem: '',
        loading: {
          subjects: false,
          skills: false,
          levels: false,
          badges: false,
        },
        currentFilterType: '',
        levels: {
          selected: '',
          available: [
            { value: '', text: 'Optionally select level' },
            { value: 1, text: 'Level 1' },
            { value: 2, text: 'Level 2' },
            { value: 3, text: 'Level 3' },
            { value: 4, text: 'Level 4' },
            { value: 5, text: 'Level 5' },
          ],
        },
        skills: {
          achieved: true,
          selected: '',
          available: [],
        },
        subjects: {
          selected: '',
          available: [],
        },
        badges: {
          selected: '',
          available: [],
        },
        criteria: {
          projectId: '',
          projectLevel: '',
          subjectLevels: [],
          badgeIds: [],
          achievedSkillIds: [],
          notAchievedSkillIds: [],
        },
      };
    },
    mounted() {
      this.loading.skills = true;
      SkillsService.getProjectSkills(this.$route.params.projectId).then((skills) => {
        this.skills.available = skills;
        this.skills.available.sort(nameSort);
      }).finally(() => {
        this.loading.skills = false;
      });

      this.loading.badges = true;

      BadgeService.getBadges(this.$route.params.projectId).then((badges) => {
        this.badges.available = badges;
        this.badges.available.sort(nameSort);
      }).finally(() => {
        this.loading.badges = false;
      });

      this.loading.subjects = true;
      SubjectService.getSubjects(this.$route.params.projectId).then((subjects) => {
        this.subjects.available = subjects;
        this.subjects.available.sort(nameSort);
      }).finally(() => {
        this.loading.subjects = false;
      });
    },
    computed: {
      formattedType() {
        let formatted = '';
        switch (this.currentFilterType) {
        case 'skill':
          formatted = 'Skills';
          break;
        case 'badge':
          formatted = 'Badges';
          break;
        case 'subject':
          formatted = 'Subjects';
          break;
        default:
          console.error(`formattedType does not support ${this.currentFilterType}`);
        }
        return formatted;
      },
      levelsDisabled() {
        let disabled = true;
        if (this.currentFilterType === 'project') {
          disabled = false;
        } else if (this.currentFilterType === 'subject' && this.subjects.selected) {
          disabled = false;
        }
        return disabled;
      },
      ids() {
        let ids = [];
        switch (this.currentFilterType) {
        case 'subject':
          ids = this.subjects.available;
          break;
        case 'badge':
          ids = this.badges.available;
          break;
        case 'skill':
          ids = this.skills.available;
          break;
        default:
          console.error(`ids does not support filter type ${this.currentFilterType}`);
        }
        return ids;
      },
      isAddDisabled() {
        let retVal = true;
        if (this.currentFilterType === 'project') {
          // can add just the project id to contact all users in the project
          retVal = false;
        } else if (this.currentFilterType === 'subject') {
          retVal = !this.subjects.selected || !this.levels.selected;
        } else if (this.currentFilterType === 'skill') {
          retVal = !this.skills.selected;
        } else if (this.currentFilterType === 'badge') {
          retVal = !this.badges.selected;
        }
        return retVal;
      },
    },
    watch: {
      currentFilterType(newVal) {
        if (newVal === 'project') {
          this.loading.levels = true;
          this.levels.selected = '';
          LevelService.getLevelsForProject(this.$route.params.projectId).then((levels) => {
            this.levels.available = levels?.map((level) => ({ value: level.level, text: level.name }));
          }).finally(() => {
            this.loading.levels = false;
          });
        } else if (newVal === 'subject') {
          // clear out levels if subject is selected, we can't load levels for a subject until the user selects the actual level
          this.levels.available = [];
          this.levels.selected = '';
        }
      },
      'subjects.selected': function subjectSelected(subject) {
        this.loading.levels = true;
        this.levels.selected = '';
        if (subject) {
          LevelService.getLevelsForSubject(this.$route.params.projectId, subject.subjectId).then((levels) => {
            this.levels.available = levels?.map((level) => ({ value: level.level, text: level.name }));
          }).finally(() => {
            this.loading.levels = false;
          });
        }
      },
      selectedItem(newVal) {
        switch (this.currentFilterType) {
        case 'subject':
          this.subjects.selected = newVal;
          break;
        case 'badge':
          this.badges.selected = newVal;
          break;
        case 'skill':
          this.skills.selected = newVal;
          break;
        default:
          console.error(`selectedItem does not support filter type ${this.currentFilterType}`);
        }
      },
    },
    methods: {
      addCriteria() {
        // need to maintain the query object
        let tag = null;

        switch (this.currentFilterType) {
        case 'project':
          if (this.levels.selected === '') {
            tag = {
              display: 'All Users',
              type: this.currentFilterType,
            };
          } else {
            tag = {
              display: `Level ${this.levels.selected} or greater`,
              type: this.currentFilterType,
              projectLevel: this.levels.selected,
            };
            this.criteria.projectLevel = this.levels.selected;
          }
          tag.projectId = this.$route.params.projectId;
          this.criteria.projectId = tag.projectId;
          break;
        case 'subject':
          tag = {
            display: `Level ${this.levels.selected} or greater in Subject ${this.subjects.selected.name}`,
            type: this.currentFilterType,
            subjectId: this.subjects.selected.subjectId,
          };
          this.criteria.subjectLevels.push({ subjectId: this.subjects.selected.subjectId, level: this.levels.selected });
          break;
        case 'skill':
          tag = {
            display: `${this.skills.achieved ? '' : 'Not '}Achieved Skill ${this.skills.selected.name}`,
            type: this.currentFilterType,
            skillId: this.skills.selected.skillId,
            achieved: this.skills.achieved,
          };
          if (this.skills.achieved) {
            this.criteria.achievedSkillIds.push(this.skills.selected.skillId);
          } else {
            this.criteria.notAchievedSkillIds.push(this.skills.selected.skillId);
          }
          break;
        case 'badge':
          tag = {
            display: `Achieved Badge ${this.badges.selected.name}`,
            badgeId: this.badges.selected.badgeId,
            type: this.currentFilterType,
          };
          this.criteria.badgeIds.push(this.badges.selected.badgeId);
          break;
        default:
          console.error(`unrecognized filter type ${this.currentFilterType}`);
        }

        if (tag) {
          this.tags.push(tag);
          this.selectedItemQuery = '';
          this.badges.selected = null;
          this.levels.selected = null;
          this.subjects.selected = null;
          this.skills.selected = null;
          this.skills.achieved = true;
        }
      },
      serializer(suggestItem) {
        let result = null;
        switch (this.currentFilterType) {
        case 'subject':
        case 'badge':
        case 'skill':
          result = suggestItem.name;
          break;
        default:
          console.error(`serializer function does not support filter type ${this.currentFilterType}`);
        }
        return result;
      },
      deleteCriteria(tag) {
        console.log('deleteCriteria clicked', tag);
        switch (tag.type) {
        case 'project':
          if (tag.projectLevel) {
            this.criteria.projectLevel = null;
          }
          break;
        case 'subject':
          // eslint-disable-next-line no-unused-expressions
          this.removeFromArray(this.criteria.subjectLevels, (el) => el.subjectId === tag.subjectId);
          break;
        case 'badge':
          // eslint-disable-next-line no-unused-expressions
          this.removeFromArray(this.criteria.badgeIds, (el) => el === tag.badgeId);
          break;
        case 'skill':
          // eslint-disable-next-line no-case-declarations
          const arrToUse = tag.achieved ? this.criteria.achievedSkillIds : this.criteria.notAchievedSkillIds;
          // eslint-disable-next-line no-unused-expressions
          this.removeFromArray(arrToUse, (el) => el === tag.skillId);
          break;
        default:
          console.error(`unrecognized user criteria type ${tag.type}`);
        }
        this.removeFromArray(this.tags, (el) => el === tag);
      },
      removeFromArray(array, findCallback) {
        const element = array.find(findCallback);
        if (element) {
          const idx = array.indexOf(element);
          array.splice(idx, 1);
        }
      },
    },
  };
</script>

<style>

</style>
