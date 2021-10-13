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
        {{ description }}
      </b-card>

      <b-card body-class="p-0 card-bg" >
        <div class="row px-3 my-2">
          <div class="col">
            <div class="row align-items-center">
              <div class="col-lg-auto border-right">
                <div v-if="this.group.enabled">
                  <span class="text-secondary">Status: </span> <span class="text-uppercase"><b-badge variant="success">Live <span class="far fa-check-circle" aria-hidden="true"/></b-badge></span>
                </div>
                <div v-if="!this.group.enabled" data-cy="skillGroupStatus" style="">
                  <span class="text-secondary">Status: </span>
                  <span class="text-uppercase mr-1"><b-badge variant="warning">Disabled</b-badge></span>
                  <span v-b-tooltip.hover="goLiveToolTipText">
                    <b-button variant="outline-info" size="sm" data-cy="selectPageOfApprovalsBtn"
                              @click="enableGroup"
                              :disabled="goLiveDisabled">
                      <i class="fas fa-glass-cheers"></i> Go Live
                    </b-button>
                  </span>
                </div>
              </div>
              <div class="col-lg mt-2">
                <b-form inline>
                  <span class="mr-1 text-secondary">Required: </span>
                  <b-badge variant="info">{{ requiredSkillsNum }}</b-badge>
                  <span class="ml-1">out <b-badge>{{ group.numSkillsInGroup }}</b-badge> skills</span>

                  <b-button variant="outline-info" size="sm"
                            @click="showEditRequiredSkillsDialog"
                            data-cy="selectPageOfApprovalsBtn" class="ml-2"><i class="far fa-edit"></i></b-button>
                </b-form>

              </div>
            </div>
          </div>
          <div class="col-auto text-right">
            <b-button :id="`group-${group.skillId}_newSkillBtn`" :ref="`group-${group.skillId}_newSkillBtn`" variant="outline-info" size="sm"
                      @click="showNewSkillDialog"
                    data-cy="newProjectButton" class="ml-1">
              <span class="">Add Skill to Group</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
            </b-button>
          </div>
        </div>
        <div class="mt-3">
          <skills-table :table-id="`groupSkills_${this.group.skillId}`" :ref="`groupSkills_${this.group.skillId}`"
                    :skills-prop="skills" :is-top-level="true"
                    :project-id="this.$route.params.projectId"
                    :subject-id="this.$route.params.subjectId"
                    @skill-removed="skillRemoved"
                    @skills-change="skillChanged"
                    :show-search="false" :show-header="false" :show-paging="false"/>
        </div>
      </b-card>
    </div>
  </loading-container>

  <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
              :project-id="editSkillInfo.skill.projectId" :subject-id="editSkillInfo.skill.subjectId" :group-id="this.group.skillId"
              @skill-saved="saveSkill" @hidden="focusOnNewSkillButton"/>
  <edit-num-required-skills v-if="editRequiredSkillsInfo.show" v-model="editRequiredSkillsInfo.show"
                            :group="group" :skills="skills" @group-changed="handleNumRequiredSkillsChanged"/>
</div>
</template>

<script>
  import SkillsService from './SkillsService';
  import EditSkill from './EditSkill';
  import LoadingContainer from '../utils/LoadingContainer';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import EditNumRequiredSkills from './skillsGroup/EditNumRequiredSkills';

  export default {
    name: 'ChildRowSkillGroupDisplay',
    mixins: [MsgBoxMixin],
    components: {
      EditNumRequiredSkills,
      LoadingContainer,
      SkillsTable: () => import('./SkillsTable'),
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
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      isLoading() {
        return this.loading.details || this.loading.skills;
      },
      goLiveDisabled() {
        return this.numSkills < 2;
      },
      goLiveToolTipText() {
        const disabled = this.numSkills < 2;
        if (disabled) {
          return 'Must have at least 2 skills to go live!';
        }
        return '';
      },
      requiredSkillsNum() {
        // -1 is disabled
        return (this.group.numSkillsRequired === -1) ? this.skills.length : this.group.numSkillsInGroup;
      },
    },
    methods: {
      loadData() {
        this.loading.skills = true;
        this.loading.details = true;

        SkillsService.getSkillDetails(this.group.projectId, this.group.subjectId, this.group.skillId)
          .then((res) => {
            this.description = res.description;
          }).finally(() => {
            this.loading.details = false;
          });

        SkillsService.getGroupSkills(this.group.projectId, this.group.skillId)
          .then((res) => {
            this.numSkills = res.length;
            this.skills = res;
          }).finally(() => {
            this.loading.skills = false;
          });
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
        this.$refs[`groupSkills_${this.group.skillId}`].skillCreatedOrUpdated(copy)
          .then(() => {
            this.numSkills += 1;
            const updatedGroup = { ...this.group, numSkillsInGroup: this.group.numSkillsInGroup + 1, numSkillsRequired: this.numSkills };
            this.$emit('group-changed', updatedGroup);
          });
      },
      skillRemoved() {
        this.numSkills -= 1;
        const updatedGroup = { ...this.group, numSkillsInGroup: this.group.numSkillsInGroup - 1, numSkillsRequired: this.numSkills };
        this.$emit('group-changed', updatedGroup);
      },
      skillChanged(skill) {
        const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
        if (item1Index >= 0) {
          this.skills.splice(item1Index, 1, skill);
        } else {
          this.skills.push(skill);
        }
      },
      handleNumRequiredSkillsChanged(updatedGroup) {
        SkillsService.saveSkill(updatedGroup).then(() => {
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
              SkillsService.saveSkill(copy).then((savedGroup) => {
                this.$emit('group-changed', savedGroup);
              });
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
