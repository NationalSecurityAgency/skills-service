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
  <loading-container v-bind:is-loading="isLoading" :data-cy="`ChildRowSkillGroupDisplay_${group.skillId}`">
    <div class="ml-4 mb-3">
      <b-card v-if="description" header="Description" class="mb-3" body-class="card-bg" data-cy="description">
        <markdown-text :text="description" />
      </b-card>

      <b-card body-class="p-0 card-bg">
        <div class="row px-3 mb-2 mt-1">
          <div class="col mt-1">
            <div class="row align-items-center">
              <div class="col-lg mt-2 mt-lg-0" data-cy="requiredSkillsSection">
                <b-form inline>
                  <span>
                    <span class="mr-1 font-italic">Required: </span>
                    <span v-if="!allSkillsRequired">
                      <b-badge variant="info" data-cy="requiredSkillsNum">{{ requiredSkillsNum }}</b-badge>
                      <span class="ml-1">out of <b-badge data-cy="numSkillsInGroup">{{
                          group.numSkillsInGroup
                        }}</b-badge> skills</span>
                    </span>
                    <span v-if="allSkillsRequired" data-cy="requiredAllSkills">
                      <b-badge variant="info" class="text-uppercase">all skills</b-badge>
                    </span>
                  </span>
                  <span v-b-tooltip.hover="editRequiredNumSkillsToolTipText">
                  <b-button variant="outline-info" size="sm"
                            @click="showEditRequiredSkillsDialog"
                            :disabled="lessThanTwoSkills"
                            :aria-label="'Edit Number of Required skills for '+ group.name + ' group'"
                            data-cy="editRequired" class="ml-2"><i class="far fa-edit"
                                                                   aria-hidden="true"></i></b-button>
                  </span>

                </b-form>

              </div>
            </div>
          </div>
          <div class="col-auto text-right mt-1">
            <b-button :id="`group-${group.skillId}_importSkillBtn`" :ref="`group-${group.skillId}_importSkillBtn`"
                      variant="outline-info" size="sm"
                      @click="importCatalog.show=true"
                      :data-cy="`importSkillToGroupBtn-${group.skillId}`" class="ml-1">
              <span class="">Import<span class="d-none d-md-inline"> Skills to Group</span></span> <i
              class="fas fa-book" aria-hidden="true"/>
            </b-button>
            <i v-if="addDisabled" class="fas fa-exclamation-circle text-warning ml-1 mr-1"
               style="pointer-events: all; font-size: 1.5rem;" v-b-tooltip.hover="addDisabledMessage"/>
            <b-button :id="`group-${group.skillId}_newSkillBtn`" :ref="`group-${group.skillId}_newSkillBtn`"
                      variant="outline-info" size="sm"
                      @click="showNewSkillDialog"
                      :disabled="addDisabled"
                      :data-cy="`addSkillToGroupBtn-${group.skillId}`" class="ml-1">
              <span class=""><span class="d-none d-md-inline">Add </span>Skill<span
                class="d-none d-md-inline"> to Group</span></span> <i class="fas fa-plus-circle" aria-hidden="true"/>
            </b-button>
          </div>
        </div>
        <hr class="w-100 mb-1"/>
        <div class="">
          <skills-table :table-id="`groupSkills_${this.group.skillId}`" :ref="`groupSkills_${this.group.skillId}`"
                        :skills-prop="skills" :is-top-level="true"
                        :project-id="this.$route.params.projectId"
                        :subject-id="this.$route.params.subjectId"
                        @skill-removed="skillRemoved"
                        @skills-change="skillChanged"
                        @skills-reused="skillReused"
                        :disableDeleteButtonsInfo="disableDeleteButtonInfo"
                        :page-size="this.maxSkillsToShow"
                        actions-btn-size="sm"
                        :show-search="false" :show-header="false" :show-paging="false"/>
        </div>
      </b-card>
    </div>
  </loading-container>

  <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :is-copy="editSkillInfo.isCopy"
              :is-edit="editSkillInfo.isEdit"
              :project-id="editSkillInfo.skill.projectId" :subject-id="editSkillInfo.skill.subjectId"
              :group-id="this.group.skillId"
              :new-skill-default-values="defaultNewSkillValues()"
              @skill-saved="saveSkill" @hidden="focusOnNewSkillButton"/>
  <edit-num-required-skills v-if="editRequiredSkillsInfo.show" v-model="editRequiredSkillsInfo.show"
                            :group="group" :skills="skills" @group-changed="handleNumRequiredSkillsChanged"
                            @skills-updated="handleSkillsUpdate"/>
  <import-from-catalog v-if="importCatalog.show" v-model="importCatalog.show" :current-project-skills="skills"
                       @to-import="importFromCatalog" @hidden="focusOnImportFromCatalogButton"/>
</div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import SkillsService from '../SkillsService';
  import EditSkill from '../EditSkill';
  import LoadingContainer from '../../utils/LoadingContainer';
  import MsgBoxMixin from '../../utils/modal/MsgBoxMixin';
  import EditNumRequiredSkills from './EditNumRequiredSkills';
  import ImportFromCatalog from '../catalog/ImportFromCatalog';
  import CatalogService from '../catalog/CatalogService';

  const {
    mapActions,
    mapGetters,
  } = createNamespacedHelpers('subjects');
  const finalizeInfo = createNamespacedHelpers('finalizeInfo');

  export default {
    name: 'ChildRowSkillGroupDisplay',
    mixins: [MsgBoxMixin],
    components: {
      ImportFromCatalog,
      MarkdownText,
      EditNumRequiredSkills,
      LoadingContainer,
      SkillsTable: () => import('../SkillsTable'),
      EditSkill,
    },
    props: {
      group: Object,
    },
    data() {
      return {
        loading: {
          details: true,
          skills: true,
        },
        numSkills: 0,
        editSkillInfo: {},
        skills: [],
        description: null,
        editRequiredSkillsInfo: {
          show: false,
        },
        importCatalog: {
          show: false,
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      isLoading() {
        return this.loading.details || this.loading.skills;
      },
      lessThanTwoSkills() {
        return this.numSkills < 2;
      },
      goLiveToolTipText() {
        const disabled = this.numSkills < 2;
        if (disabled) {
          return 'Must have at least 2 skills to go live!';
        }
        return '';
      },
      editRequiredNumSkillsToolTipText() {
        const disabled = this.numSkills < 2;
        if (disabled) {
          return 'Must have at least 2 skills to modify!';
        }
        return '';
      },
      requiredSkillsNum() {
        // -1 == all skills required
        return (this.group.numSkillsRequired === -1) ? this.skills.length : this.group.numSkillsRequired;
      },
      allSkillsRequired() {
        // -1 == all skills required
        return (this.group.numSkillsRequired < 0);
      },
      disableDeleteButtonInfo() {
        let res = null;
        if (this.group.numSkillsRequired > 0 && this.group.numSkillsRequired === this.skills.length) {
          res = {
            minNumSkills: this.group.numSkillsRequired,
            tooltip: 'Cannot delete! Cannot go below the number of the required skills.',
          };
        }
        return res;
      },
      maxSkillsToShow() {
        if (this.$store.getters.config) {
          return Number(this.$store.getters.config.maxSkillsPerSubject);
        }
        return 10;
      },
      goLiveDisabled() {
        if (this.$store.getters.config) {
          if (this.group.enabled) {
            return this.subject.numSkills >= this.$store.getters.config.maxSkillsPerSubject;
          }
          return this.subject.numSkills + this.numSkills > this.$store.getters.config.maxSkillsPerSubject;
        }
        return false;
      },
      addDisabled() {
        if (this.group.enabled) {
          if (this.$store.getters.config && this.subject.numSkills >= this.$store.getters.config.maxSkillsPerSubject) {
            return true;
          }
        }
        return false;
      },
      addDisabledMessage() {
        return `No more Skills can be added to this group, the maximum number of Skills allowed per subject is ${this.$store.getters.config.maxSkillsPerSubject}`;
      },
      disabledMessage() {
        return `This group cannot be enabled. The maximum number of Skills allowed per subject is ${this.$store.getters.config.maxSkillsPerSubject}.`;
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      ...finalizeInfo.mapActions([
        'loadFinalizeInfo',
      ]),
      loadData() {
        this.loading.skills = true;
        this.loading.details = true;

        SkillsService.getSkillDetails(this.group.projectId, this.group.subjectId, this.group.skillId)
          .then((res) => {
            this.description = res.description;
          })
          .finally(() => {
            this.loading.details = false;
          });

        this.loadGroupSkills();
      },
      loadGroupSkills() {
        this.loading.skills = true;
        return SkillsService.getGroupSkills(this.group.projectId, this.group.skillId)
          .then((res) => {
            this.setInternalSkills(res);
          })
          .finally(() => {
            this.loading.skills = false;
          });
      },
      handleSkillsUpdate(skills) {
        this.setInternalSkills(skills);
        this.$refs[`groupSkills_${this.group.skillId}`].loadDataFromParams(skills);
        this.refreshSubjectState();
      },
      setInternalSkills(skillsParam) {
        this.numSkills = skillsParam.length;
        this.skills = skillsParam.map((skill) => ({
          ...skill,
          subjectId: this.group.subjectId,
        }));
      },
      showNewSkillDialog() {
        this.editSkillInfo = {
          skill: {
            projectId: this.group.projectId,
            subjectId: this.group.subjectId,
            type: 'Skill',
          },
          show: true,
          isEdit: false,
          isCopy: false,
        };
      },
      showEditRequiredSkillsDialog() {
        this.editRequiredSkillsInfo = {
          show: true,
        };
      },
      saveSkill(skill) {
        const copy = { groupId: this.group.skillId, ...skill };
        this.$refs[`groupSkills_${this.group.skillId}`].skillCreatedOrUpdated(copy);
        // credit is only given if skill is added to a group
        SkillsReporter.reportSkill('CreateSkillGroup');
      },
      refreshSubjectState(forceRefresh = false) {
        if (this.group.enabled || forceRefresh) {
          this.loadSubjectDetailsState({ projectId: this.group.projectId, subjectId: this.group.subjectId });
        }
      },
      skillRemoved(skill) {
        this.numSkills -= 1;
        this.skills = this.skills.filter((item) => item.skillId !== skill.skillId);
        const updatedGroup = {
          ...this.group,
          numSkillsInGroup: this.group.numSkillsInGroup - 1,
          numSkillsRequired: this.group.numSkillsRequired === this.numSkills ? -1 : this.group.numSkillsRequired,
          totalPoints: this.group.totalPoints - (skill.pointIncrement * skill.numPerformToCompletion),
        };
        this.$emit('group-changed', updatedGroup);
        this.refreshSubjectState();
      },
      skillReused(reused) {
        this.$emit('skills-reused', reused);
      },
      skillChanged(skill) {
        const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
        if (item1Index >= 0) {
          const removedSkill = this.skills[item1Index];
          this.skills.splice(item1Index, 1, skill);
          const updatedGroup = {
            ...this.group,
            totalPoints: this.group.totalPoints - (removedSkill.pointIncrement * removedSkill.numPerformToCompletion) + (skill.pointIncrement * skill.numPerformToCompletion),
          };
          this.$emit('group-changed', updatedGroup);
        } else {
          this.skills.push(skill);
          this.numSkills = this.skills.length;

          const updatedGroup = {
            ...this.group,
            numSkillsInGroup: this.group.numSkillsInGroup + 1,
            numSkillsRequired: this.group.numSkillsRequired,
            totalPoints: this.group.totalPoints + (skill.pointIncrement * skill.numPerformToCompletion),
          };
          this.$emit('group-changed', updatedGroup);
          this.refreshSubjectState();
        }
        this.refreshSubjectState();
      },
      handleNumRequiredSkillsChanged(updatedGroup) {
        SkillsService.saveSkill(updatedGroup)
          .catch((err) => {
            if (err && err.response && err.response.data.errorCode === 'MaxSkillsThreshold') {
              this.msgOk(err.response.data.explanation, 'Maximum Skills Reached');
            } else {
              throw err;
            }
          })
          .then(() => {
            this.$emit('group-changed', updatedGroup);
          });
      },
      focusOnNewSkillButton() {
        const ref = this.$refs[`group-${this.group.skillId}_newSkillBtn`];
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      enableGroup() {
        const msg = `While this Group is disabled, user's cannot see the group or achieve it. Once the group is live, it will be visible to users.
        Please note that once the group is live, it cannot be disabled.`;
        this.msgConfirm(msg, 'Please Confirm!', 'Yes, Go Live!')
          .then((res) => {
            if (res) {
              const copy = { ...this.group, enabled: true };
              SkillsService.saveSkill(copy)
                .catch((err) => {
                  if (err && err.response && err.response.data.errorCode === 'MaxSkillsThreshold') {
                    this.msgOk(err.response.data.explanation, 'Maximum Skills Reached');
                  } else {
                    throw err;
                  }
                })
                .then((savedGroup) => {
                  this.$emit('group-changed', savedGroup);
                  this.refreshSubjectState(true);
                });
            }
          });
      },
      defaultNewSkillValues() {
        if (this.group.numSkillsRequired === -1 || !this.skills || this.skills.length === 0) {
          return null;
        }
        return {
          pointIncrement: this.skills[0].pointIncrement,
          numPerformToCompletion: this.skills[0].numPerformToCompletion,
        };
      },
      importFromCatalog(skillsInfoToImport) {
        this.loading.skills = true;
        CatalogService.bulkImportIntoGroup(this.$route.params.projectId, this.$route.params.subjectId, this.group.skillId, skillsInfoToImport)
          .then(() => {
            this.loadGroupSkills()
              .then(() => {
                this.focusOnImportFromCatalogButton();
              });
            this.loadSubjectDetailsState({
              projectId: this.$route.params.projectId,
              subjectId: this.$route.params.subjectId,
            });
            this.loadFinalizeInfo({ projectId: this.$route.params.projectId });
          });
      },
      focusOnImportFromCatalogButton() {
        const ref = this.$refs[`group-${this.group.skillId}_importSkillBtn`];
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };
</script>

<style scoped>
.card-bg {
  background-color: rgba(0,124,73,0.04) !important;
}
</style>
