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
  <metrics-card id="prerequisite-selector-panel" title="Add a new item to the learning path"
                :no-padding="true" data-cy="addPrerequisiteToLearningPath" style="margin-bottom:10px;">
    <ValidationObserver ref="validationObserver" v-slot="{ invalid }">
    <b-overlay :show="isLoading" rounded="sm" opacity="0.2">
      <div class="row ml-1 mr-3 my-2 no-gutters">
        <div class="col-lg ml-2 mt-1">
          From:
          <skills-selector2 :options="allSkills" v-on:added="onFromSelected" v-on:removed="onFromDeselected"
                            @selection-removed="onFromSelectionRemoved"
                            :selected="selectedFromSkills" :onlySingleSelectedValue="true" placeholder="Select a Skill or Badge"
                            :showType=true data-cy="learningPathFromSkillSelector"></skills-selector2>
        </div>
        <div class="col-lg mt-1 ml-2">
          To:
          <skills-selector2 :options="allPotentialSkills" v-on:added="onToSelected" v-on:removed="onToDeselected"
                            @selection-removed="onToSelectionRemoved" :disabled="selectedFromSkills.length === 0"
                            :selected="selectedToSkills" :onlySingleSelectedValue="true" placeholder="Select a Skill or Badge"
                            :showType=true data-cy="learningPathToSkillSelector"></skills-selector2>
        </div>
        <div class="col-lg-auto text-right mt-1 ml-2 align-self-end">
          <button type="button"
                  class="btn btn-info btn-floating skills-theme-btn" @click="onAddPath"
                  data-cy="addLearningPathItemBtn"
                  :disabled="selectedFromSkills.length === 0 || !toSkillId || invalid">Add <i class="fas fa-plus-circle" aria-hidden="true"/></button>
        </div>
      </div>
    </b-overlay>

      <ValidationProvider ref="learningPathValidator" :immediate="true"
                          rules="validLearningPath" v-slot="{errors, valid}" name="Skill Name">
        <input v-model="toSkillId" class="d-none"/>
        <div v-if="!valid" class="mx-3 alert alert-danger" data-cy="learningPathError"><i class="fas fa-exclamation-triangle" aria-hidden="true"/><span v-html="errors[0]" class="px-3"/></div>
      </ValidationProvider>

    </ValidationObserver>
  </metrics-card>
</template>

<script>
  import { extend } from 'vee-validate';
  import MetricsCard from '@/components/metrics/utils/MetricsCard';
  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';
  import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService';

  export default {
    name: 'PrerequisiteSelector',
    props: ['projectId', 'selectedFromSkills'],
    components: {
      MetricsCard,
      SkillsSelector2,
    },
    data() {
      return {
        tempText: '',
        allSkills: [],
        allPotentialSkills: [],
        selectedToSkills: [],
        toSkillId: null,
        toSkillName: null,
        toProjectId: null,
        loadingPotentialSkills: false,
        loadingAllSkills: false,
      };
    },
    watch: {
      selectedFromSkills: function skillWatch() {
        this.clearToData();
        this.updatePotentialSkills();
      },
    },
    mounted() {
      this.loadAllSkills();
      this.registerValidation();
    },
    computed: {
      isLoading() {
        return this.loadingPotentialSkills || this.loadingAllSkills;
      },
    },
    methods: {
      loadAllSkills() {
        this.loadingAllSkills = true;
        const getProjectSkillsAndBadges = SkillsService.getProjectSkillsAndBadgesWithImportedSkills(this.projectId);
        const getSharedSkills = SkillsShareService.getSharedWithmeSkills(this.projectId);

        Promise.all([getProjectSkillsAndBadges, getSharedSkills]).then((results) => {
          const allSkills = results[0];
          const sharedSkills = results[1];
          if (sharedSkills && sharedSkills.length > 0) {
            sharedSkills.forEach((skill) => {
              const newSkill = {
                name: skill.skillName,
                type: 'Shared Skill',
                ...skill,
              };
              allSkills.push(newSkill);
            });
          }
          this.allSkills = allSkills;
          this.loadingAllSkills = false;
        });
      },
      updatePotentialSkills() {
        this.loadingPotentialSkills = true;

        SkillsService.getProjectSkillsAndBadgesWithImportedSkills(this.projectId)
          .then((skills) => {
            if (this.selectedFromSkills.length > 0 && this.selectedFromSkills[0].skillId) {
              this.allPotentialSkills = skills.filter((skill) => (skill.skillId !== this.selectedFromSkills[0].skillId || (skill.skillId === this.selectedFromSkills[0].skillId && skill.projectId !== this.selectedFromSkills[0].projectId)));
            }
            if (this.selectedToSkills.length > 0) {
              this.selectedToSkills.forEach((skill) => {
                this.allPotentialSkills = this.allPotentialSkills.filter((potentialSkill) => (potentialSkill.skillId !== skill.skillId || (potentialSkill.skillId === skill.skillId && potentialSkill.projectId !== skill.projectId)));
              });
            }
            this.loadingPotentialSkills = false;
          });
      },
      onToSelected(item) {
        this.toSkillId = item.skillId;
        this.toSkillName = item.name;
        this.toProjectId = item.projectId;
      },
      onToDeselected() {
        this.selectedToSkills = [];
        this.updatePotentialSkills();
      },
      onFromSelectionRemoved() {
        if (this.$refs && this.$refs.learningPathValidator) {
          this.clearData();
          this.$refs.learningPathValidator.reset();
        }
      },
      onToSelectionRemoved() {
        if (this.$refs && this.$refs.learningPathValidator) {
          this.clearToData();
          this.$refs.learningPathValidator.reset();
          this.updatePotentialSkills();
        }
      },
      onFromSelected(item) {
        this.clearToData();
        this.$emit('updateSelectedFromSkills', item);
      },
      onFromDeselected() {
      },
      onAddPath() {
        this.$refs.validationObserver.validate()
          .then((res) => {
            if (res) {
              SkillsService.assignDependency(this.toProjectId, this.toSkillId, this.selectedFromSkills[0].skillId, this.selectedFromSkills[0].projectId)
                .then(() => {
                  this.clearData();
                  this.$emit('update');
                });
            }
          });
      },
      clearData() {
        this.$emit('clearSelectedFromSkills');
        this.clearToData();
      },
      clearToData() {
        this.allPotentialSkills = [];
        this.selectedToSkills = [];
        this.toSkillId = null;
        this.toSkillName = null;
        this.toProjectId = null;
      },
      registerValidation() {
        const self = this;
        extend('validLearningPath', {
          validate() {
            if (!self || !self.toProjectId || !self.toSkillId || !self.selectedFromSkills[0].skillId || !self.selectedFromSkills[0].projectId) {
              return true;
            }
            return SkillsService.validateDependency(self.toProjectId, self.toSkillId, self.selectedFromSkills[0].skillId, self.selectedFromSkills[0].projectId)
              .then((res) => {
                if (res.possible) {
                  return true;
                }

                if (res.failureType && res.failureType === 'CircularLearningPath') {
                  const additionalBadgeMsg = res.violatingSkillInBadgeName ? `under the badge <b>${res.violatingSkillInBadgeName}</b> ` : '';
                  return `<b>${self.toSkillName}</b> already exists in the learning path ${additionalBadgeMsg}and adding it again will cause a <b>circular/infinite learning path</b>.`;
                }
                if (res.failureType && res.failureType === 'BadgeOverlappingSkills') {
                  return 'Multiple badges on the same Learning path cannot have overlapping skills. '
                    + `Both <b>${res.violatingSkillInBadgeName}</b> badge and <b>${self.toSkillName}</b> badge have <b>${res.violatingSkillName}</b> skill.`;
                }
                if (res.failureType && res.failureType === 'BadgeSkillIsAlreadyOnPath') {
                  return `Provided badge <b>${self.toSkillName}</b> has skill <b>${res.violatingSkillName}</b> which already exists on the learning path.`;
                }
                if (res.failureType && res.failureType === 'AlreadyExist') {
                  return `Learning path from <b>${res.violatingSkillName}</b> to <b>${self.toSkillName}</b> already exists.`;
                }
                if (res.failureType && res.failureType === 'SkillInCatalog') {
                  return `Skill <b>${self.toSkillName}</b> was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.`;
                }
                if (res.failureType && res.failureType === 'ReusedSkill') {
                  return `Skill <b>${self.toSkillName}</b> was reused in another subject or group and cannot have prerequisites in the learning path.`;
                }

                return `${res.reason}`;
              });
          },
        });
      },
    },
  };
</script>

<style>

</style>
