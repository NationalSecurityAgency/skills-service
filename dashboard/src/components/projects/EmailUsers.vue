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
      <b-overlay :show="!emailFeatureConfigured">
        <div slot="overlay" class="alert alert-warning mt-2" data-cy="contactUsers_emailServiceWarning">
          <i class="fa fa-exclamation-triangle" aria-hidden="true"/> Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </div>
        <div class="h5 pt-2 mt-2 pr-2 pl-2 mr-2 ml-2 text-uppercase">Filters</div>
        <div class="row p-2 m-2">
          <div class="col-12 col-md-3 col-lg-2 border-md-right">
              <b-form-group label="Type:" label-for="filter-type" label-class="text-muted">
                <b-form-radio-group id="type-radio-group" v-model="currentFilterType" name="filter-type-options" :disabled="criteria.allProjectUsers" stacked>
                  <b-form-radio value="project" data-cy="projectFilter">Project</b-form-radio>
                  <b-form-radio value="badge" data-cy="badgeFilter">Badge</b-form-radio>
                  <b-form-radio value="subject" data-cy="subjectFilter">Subject</b-form-radio>
                  <b-form-radio value="skill" data-cy="skillFilter">Skill</b-form-radio>
                </b-form-radio-group>
              </b-form-group>
            </div>
          <div class="col-9 col-lg-10">
            <b-form-group label="Name (Subject, Skill and Badge Only):" label-for="name-filter" label-class="text-muted">
              <b-overlay :show="loading.skills || loading.badges || loading.subjects" rounded="sm" opacity="0.5"
                         spinner-variant="info" spinner-type="grow" spinner-small>
                <template v-if="currentFilterType && currentFilterType !== 'project'">

                  <multiselect ref="select" v-model="selectedItem" :options="ids" label="name" :searchable="true"
                               :loading="isLoading" trackBy="name"
                              :placeholder="selectText" :multiple="false"/>

                 <b-form-group label="Achieved" label-for="achieved-button" label-class="text-muted" v-show="currentFilterType && currentFilterType==='skill'"
                               class="mt-4" :disabled="criteria.allProjectUsers">
                    <b-form-checkbox v-model="skills.achieved"
                                     name="achieved-button"
                                     data-cy="skillAchievedSwitch"
                                     switch>
                      {{ skills.achieved ? 'Achieved' : 'Not Achieved' }}
                    </b-form-checkbox>
                  </b-form-group>
                </template>
                <b-form-input v-else disabled/>
              </b-overlay>

            </b-form-group>
            <div class="row p-3">
              <b-form-group id="levels-input-group" label="Minimum Level (Project & Subject Only):" label-for="input-3" label-class="text-muted">
                <b-form-select id="input-3" v-model="levels.selected" :options="levels.available"
                               required data-cy="emailUsers-levelsInput"
                               :disabled="levelsDisabled || criteria.allProjectUsers" />
              </b-form-group>
          </div>
        </div>
        </div>
        <div class="row pl-3 pr-3 pt-1 pb-1 ml-3 mr-3 mt-1 mb-1">
          <b-button variant="outline-primary" class="mr-1" @click="addCriteria" data-cy="emailUsers-addBtn"
                    :disabled="isAddDisabled || maxTagsReached"><i class="fas fa-plus-circle"/> Add</b-button>
          <transition name="fade">
            <span v-if="alreadyApplied" data-cy="filterExists" class="pt-2 pl-1">Filter already exists</span>
          </transition>
          <span v-if="maxTagsReached" data-cy="maxFiltersReached" class="text-warning pt-2 pl-1">Only {{maxCriteria}} filters are allowed</span>
        </div>

        <div class="container-fluid pl-3 pr-3 pb-3 pt-1 mr-3 ml-1">
          <div class="pb-3 mt-1 ml-3">
            <b-badge v-for="(tag) of tags" :key="tag.display" variant="info" class="pl-2 m-2 text-break"
                     style="max-width: 85%;" data-cy="filterBadge">
              {{tag.display}} <b-button @click="deleteCriteria(tag)"
                                        variant="outline-info" size="sm" class="text-warning"
                                        :aria-label="`Remove contact user criteria ${tag.display}`"
                                        data-cy="contactUserCriteria-removeBtn"><i class="fa fa-trash" /><span class="sr-only">delete filter {{tag.display}}</span></b-button>
            </b-badge>
          </div>
          <h1 class="ml-4 h5 text-uppercase" data-cy="usersMatchingFilters"><b-badge variant="info">{{this.currentCount|number}}</b-badge> Users Selected</h1>
        </div>

        <hr />
        <div class="pl-2 ml-2 pr-2 mr-2 h5 pb-2 text-uppercase">Email Content</div>
        <div class="row pl-3 pr-3 pt-3 pb-1 m-3 mb-1">
          <b-form-group class="w-100" id="subject-line-input-group" label="Subject Line" label-for="subject-line-input" label-class="text-muted">
            <b-input class="w-100" v-model="subject" id="subject-line-input" data-cy="emailUsers_subject"/>
          </b-form-group>
        </div>
        <div class="row pl-3 pr-3 pb-1 ml-3 mr-3 mb-1 mt-1">
          <b-form-group class="w-100" id="body-input-group" label="Email Body" label-for="body-input" label-class="text-muted">
            <markdown-editor class="w-100" v-model="body" data-cy="emailUsers_body"/>
          </b-form-group>
        </div>
        <div class="row pl-3 pr-3 pb-3 pt-1 ml-3 mr-3 mb-3 mt-1">
          <b-button class="mr-3" data-cy="previewUsersEmail"
                    :disabled="isPreviewDisabled"
                    @click="previewEmail"
                    variant="outline-primary" aria-label="preview email to project users">
            <span>Preview</span> <i :class="[emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-eye']" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
          <b-button variant="outline-primary" class="mr-1" @click="emailUsers" data-cy="emailUsers-submitBtn"
                    :disabled="isEmailDisabled"><i :class="[emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fas fa-mail-bulk']" /> Email</b-button>
          <transition name="fade">
            <span v-if="emailSent" class="pt-2 pl-1"><i class="far fa-check-square text-success"/> {{ sentMsg }}</span>
          </transition>
        </div>
      </b-overlay>
    </b-card>
  </div>

</template>

<script>
  import Multiselect from 'vue-multiselect';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import SkillsService from '../skills/SkillsService';
  import LevelService from '../levels/LevelService';
  import BadgeService from '../badges/BadgesService';
  import SubjectService from '../subjects/SubjectsService';
  import ProjectService from './ProjectService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

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
      SubPageHeader,
      MarkdownEditor,
      Multiselect,
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        sentMsg: '',
        emailFeatureConfigured: true,
        maxCriteria: 15,
        alreadyApplied: false,
        currentCount: 0,
        tags: [],
        subject: '',
        body: '',
        selectedItem: '',
        emailSent: false,
        emailing: false,
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
          allProjectUsers: false,
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

      ProjectService.isEmailServiceSupported().then((emailEnabled) => {
        this.emailFeatureConfigured = emailEnabled;
      });
    },
    computed: {
      formattedType() {
        return this.currentFilterType.charAt(0).toUpperCase() + this.currentFilterType.slice(1);
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
          // eslint-disable-next-line no-console
          console.error(`ids does not support filter type ${this.currentFilterType}`);
        }
        return ids;
      },
      isAddDisabled() {
        if (this.criteria.allProjectUsers) {
          return true;
        }
        let disabled = true;
        if (this.currentFilterType === 'project') {
          // can add just the project id to contact all users in the project
          disabled = false;
        } else if (this.currentFilterType === 'subject') {
          disabled = !this.subjects.selected || !this.levels.selected;
        } else if (this.currentFilterType === 'skill') {
          disabled = !this.skills.selected;
        } else if (this.currentFilterType === 'badge') {
          disabled = !this.badges.selected;
        }
        return disabled;
      },
      maxTagsReached() {
        return this.tags.length === this.maxCriteria;
      },
      isEmailDisabled() {
        return !this.body || !this.subject || this.emailing || this.emailSent || this.tags.length < 1 || this.currentCount < 1;
      },
      isPreviewDisabled() {
        return !this.body || !this.subject;
      },
      isLoading() {
        return this.loading.skills || this.loading.badges;
      },
      selectText() {
        let text = '';
        if (this.currentFilterType === 'skill') {
          text = 'Select Skill';
        } else if (this.currentFilterType === 'badge') {
          text = 'Select Badge';
        } else if (this.currentFilterType === 'subject') {
          text = 'Select Subject';
        }
        return text;
      },
    },
    watch: {
      currentFilterType(newVal) {
        if (newVal === 'project') {
          this.loading.levels = true;
          this.levels.selected = '';
          LevelService.getLevelsForProject(this.$route.params.projectId).then((levels) => {
            this.levels.available = levels?.map((level) => ({ value: level.level, text: level.level }));
            this.levels.available.unshift({ value: null, text: 'Any Level' });
            this.levels.selected = null;
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
            this.levels.available = levels?.map((level) => ({ value: level.level, text: level.level }));
          }).finally(() => {
            this.loading.levels = false;
          });
        }
      },
      selectedItem(newVal) {
        if (!newVal) {
          return;
        }

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
          // eslint-disable-next-line no-console
          console.error(`selectedItem does not support filter type ${this.currentFilterType}, value [${newVal}]`);
        }
      },
    },
    methods: {
      addCriteria() {
        let tag = null;
        const c = this.criteria;
        switch (this.currentFilterType) {
        case 'project':
          if (this.levels.selected === '' || this.levels.selected === null) {
            tag = {
              display: 'All Users',
              type: this.currentFilterType,
            };
            this.criteria.allProjectUsers = true;
          } else {
            tag = {
              display: `Level ${this.levels.selected} or greater`,
              type: this.currentFilterType,
              projectLevel: this.levels.selected,
            };
            c.projectLevel = this.levels.selected;
          }
          tag.projectId = this.$route.params.projectId;
          c.projectId = tag.projectId;
          break;
        case 'subject':
          tag = {
            display: `Level ${this.levels.selected} or greater in Subject ${this.subjects.selected.name}`,
            type: this.currentFilterType,
            subjectId: this.subjects.selected.subjectId,
          };
          // eslint-disable-next-line no-case-declarations
          const slC = { subjectId: this.subjects.selected.subjectId, level: this.levels.selected };
          if (!this.arrayContainsObject(c, slC)) {
            c.subjectLevels.push(slC);
          }
          break;
        case 'skill':
          // eslint-disable-next-line no-case-declarations
          const selectedId = this.skills.selected.skillId;
          tag = {
            display: `${this.skills.achieved ? '' : 'Not '}Achieved Skill ${this.skills.selected.name}`,
            type: this.currentFilterType,
            skillId: selectedId,
            achieved: this.skills.achieved,
          };
          if (this.skills.achieved && c.achievedSkillIds.indexOf(selectedId) < 0) {
            c.achievedSkillIds.push(selectedId);
          } else if (c.notAchievedSkillIds.indexOf(selectedId) < 0) {
            c.notAchievedSkillIds.push(selectedId);
          }
          break;
        case 'badge':
          // eslint-disable-next-line no-case-declarations
          const selectedBadgeId = this.badges.selected.badgeId;
          tag = {
            display: `Achieved Badge ${this.badges.selected.name}`,
            badgeId: selectedBadgeId,
            type: this.currentFilterType,
          };
          if (c.badgeIds.indexOf(selectedId) < 0) {
            c.badgeIds.push(selectedBadgeId);
          }
          break;
        default:
          // eslint-disable-next-line no-console
          console.error(`unrecognized filter type ${this.currentFilterType}`);
        }

        this.handleTagAdd(tag);
      },
      handleTagAdd(tag) {
        if (!tag) {
          return;
        }
        const contained = this.tagAlreadyExists(tag);
        if (contained) {
          this.alreadyApplied = true;
          setTimeout(() => { this.alreadyApplied = false; }, 2000);
          return;
        }
        const addTagAndUpdate = () => {
          this.updateCount();
          this.tags.push(tag);
          this.resetSelections();
        };
        if (this.criteria.allProjectUsers) {
          if (this.tags.length > 0) {
            this.msgConfirm(
              'Adding the All Users filter will remove all other filters',
              'Remove Other Filters?',
              'YES, Remove Them!',
            ).then((res) => {
              if (res) {
                this.resetCriteria(true);
                this.resetTags();
                addTagAndUpdate();
              } else {
                this.criteria.allProjectUsers = false;
              }
            });
          } else {
            this.resetCriteria(true);
            this.resetTags();
            addTagAndUpdate();
          }
        } else {
          addTagAndUpdate();
        }
      },
      resetSelections() {
        this.badges.selected = null;
        this.levels.selected = null;
        this.subjects.selected = null;
        this.skills.selected = null;
        this.skills.achieved = true;
        this.selectedItem = null;
      },
      resetCriteria(allProjects) {
        this.criteria.projectLevel = '';
        this.criteria.subjectLevels = [];
        this.criteria.badgeIds = [];
        this.criteria.achievedSkillIds = [];
        this.criteria.notAchievedSkillIds = [];
        this.criteria.allProjectUsers = allProjects;
      },
      resetTags() {
        this.tags.splice(0, this.tags.length);
      },
      tagAlreadyExists(tag) {
        const exists = this.arrayContainsObject(this.tags, tag);
        return exists;
      },
      arrayContainsObject(array, object) {
        let exists = false;
        const searchObj = JSON.stringify(object);
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < array.length; i++) {
          const t = array[i];
          if (JSON.stringify(t) === searchObj) {
            exists = true;
            break;
          }
        }
        return exists;
      },
      deleteCriteria(tag) {
        switch (tag.type) {
        case 'project':
          if (tag.projectLevel) {
            this.criteria.projectLevel = null;
          } else {
            this.criteria.allProjectUsers = false;
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
          // eslint-disable-next-line no-console
          console.error(`unrecognized user criteria type ${tag.type}`);
        }
        this.removeFromArray(this.tags, (el) => el === tag);
        this.updateCount();
      },
      removeFromArray(array, findCallback) {
        const element = array.find(findCallback);
        if (element) {
          const idx = array.indexOf(element);
          array.splice(idx, 1);
        }
      },
      updateCount() {
        ProjectService.countUsersMatchingCriteria(this.$route.params.projectId, this.criteria).then((count) => {
          this.currentCount = count;
        });
      },
      emailUsers() {
        this.emailing = true;
        this.sentMsg = `Email${this.currentCount > 1 ? 's' : ''} sent!`;
        ProjectService.contactUsers(this.$route.params.projectId, {
          queryCriteria: this.criteria,
          emailBody: this.body,
          emailSubject: this.subject,
        }).then(() => {
          this.emailSent = true;
          this.$nextTick(() => {
            this.resetTags();
            this.resetCriteria();
            this.body = '';
            this.subject = '';
            this.currentCount = 0;
          });
          setTimeout(() => { this.emailSent = false; }, 8000);
        }).finally(() => {
          this.emailing = false;
        });
      },
      previewEmail() {
        this.emailing = true;
        this.sentMsg = 'Preview email sent!';
        ProjectService.previewEmail(this.$route.params.projectId, {
          emailBody: this.body,
          emailSubject: this.subject,
        }).then(() => {
          this.emailSent = true;
          setTimeout(() => { this.emailSent = false; }, 8000);
        }).finally(() => {
          this.emailing = false;
        });
      },
    },
  };
</script>

<style>
  .fade-enter-active {
    transition: opacity .5s;
  }
  .fade-leave-active {
    transition: opacity 2s;
  }
  .fade-enter, .fade-leave-to {
    opacity: 0;
  }
</style>
