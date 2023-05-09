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
    <div class="row px-3 py-2">
      <div class="col-6">
        From:
        <skills-selector2 :options="allSkills" v-on:added="onFromSelected" v-on:removed="onFromDeselected"
                          :selected="selectedFromSkills" :onlySingleSelectedValue="true" placeholder="Select a Skill or Badge"
                          data-cy="skillSelectorPrerequisites"></skills-selector2>
      </div>
      <div class="col-5">
        To:
        <skills-selector2 :options="allPotentialSkills" v-on:added="onToSelected" v-on:removed="onToDeselected"
                          :selected="selectedToSkills" :onlySingleSelectedValue="true" placeholder="Select a Skill or Badge"
                          data-cy="skillSelector"></skills-selector2>
      </div>
      <div class="col-1" style="margin-top: 24px;">
        <button type="button" class="btn btn-info btn-floating skills-theme-btn" @click="onAddPath" :disabled="!this.fromSkillId || !this.toSkillId">Add</button>
      </div>
    </div>
  </metrics-card>
</template>

<script>
  import MetricsCard from '@/components/metrics/utils/MetricsCard';
  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';

  export default {
    name: 'PrerequisiteSelector',
    props: ['projectId'],
    components: {
      MetricsCard,
      SkillsSelector2,
    },
    data() {
      return {
        selectedFromSkills: [],
        allSkills: [],
        allPotentialSkills: [],
        selectedToSkills: [],
        fromSkillId: null,
        fromProjectId: null,
        toSkillId: null,
        toProjectId: null,
      };
    },
    mounted() {
      this.loadAllSkills();
    },
    methods: {
      loadAllSkills() {
        SkillsService.getProjectSkillsAndBadgesWithImportedSkills(this.projectId)
          .then((skills) => {
            this.allSkills = skills;
          });
      },
      updatePotentialSkills() {
        SkillsService.getProjectSkillsAndBadgesWithImportedSkills(this.projectId)
          .then((skills) => {
            if (this.fromSkillId) {
              this.allPotentialSkills = skills.filter((skill) => skill.skillId !== this.fromSkillId);
            }
            if (this.selectedToSkills.length > 0) {
              this.selectedToSkills.forEach((skill) => {
                this.allPotentialSkills = this.allPotentialSkills.filter((potentialSkill) => potentialSkill.skillId !== skill.skillId);
              });
            }
          });
      },
      onToSelected(item) {
        this.toSkillId = item.skillId;
        this.toProjectId = item.projectId;
      },
      onToDeselected() {
        this.selectedToSkills = [];
        this.updatePotentialSkills();
      },
      onFromSelected(item) {
        this.clearToData();
        this.selectedFromSkills = [item];
        this.fromSkillId = item.skillId;
        this.fromProjectId = item.projectId;
        this.updatePotentialSkills();
      },
      onFromDeselected() {
        console.log('Deselected');
      },
      onAddPath() {
        SkillsService.assignDependency(this.toProjectId, this.toSkillId, this.fromSkillId, this.fromProjectId).then(() => {
          this.clearData();
          this.$emit('update');
        });
      },
      clearData() {
        this.selectedFromSkills = [];
        this.fromSkillId = null;
        this.fromProjectId = null;
        this.clearToData();
      },
      clearToData() {
        this.allPotentialSkills = [];
        this.selectedToSkills = [];
        this.toSkillId = null;
        this.toProjectId = null;
      },
    },
  };
</script>

<style>

</style>
