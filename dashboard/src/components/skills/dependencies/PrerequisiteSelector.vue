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
  <metrics-card id="prerequisite-selector-panel" title="Add a new skill to the learning path"
                :no-padding="true" data-cy="addPrerequisiteToLearningPath">
    <div class="row px-3 py-2">
      <div class="col">
        Skill to add to learning path:
        <skills-selector2 :options="allSkills" v-on:added="onSelectedSkill" v-on:removed="onDeselectedSkill"
                          :selected="selectedSkills" :onlySingleSelectedValue="true"
                          data-cy="skillSelector"></skills-selector2>
      </div>
    </div>

    <div v-if="selectedSkills.length > 0" class="row px-3 py-3">
      <div class="col">
        Skills to add as prerequisite(s):
        <skills-selector2 :options="allPotentialSkills" v-on:added="onPrerequisiteSelected" v-on:removed="onPrerequisiteDeselected"
                          :selected="selectedPrerequisites" :onlySingleSelectedValue="false"
                          data-cy="skillSelectorPrerequisites"></skills-selector2>
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
        selectedSkills: [],
        allSkills: [],
        allPotentialSkills: [],
        selectedPrerequisites: [],
      };
    },
    mounted() {
      this.loadAllSkills();
    },
    methods: {
      loadAllSkills() {
        // this.loading.allSkills = true;
        SkillsService.getProjectSkillsWithoutImportedSkills(this.projectId)
          .then((skills) => {
            this.allSkills = skills;
            this.updatePotentialSkills();
            // this.loading.allSkills = false;
          });
      },
      updatePotentialSkills() {
        SkillsService.getProjectSkillsAndBadgesWithoutImportedSkills(this.projectId)
          .then((skills) => {
            if (this.skillId) {
              this.allPotentialSkills = skills.filter((skill) => skill.skillId !== this.skillId);
            }
            if (this.selectedPrerequisites.length > 0) {
              this.selectedPrerequisites.forEach((skill) => {
                this.allPotentialSkills = this.allPotentialSkills.filter((potentialSkill) => potentialSkill.skillId !== skill.skillId);
              });
            }
          });
      },
      onSelectedSkill(item) {
        this.selectedSkills = [item];
        this.skillId = item.skillId;
        this.loadDataForSkill();
      },
      onDeselectedSkill() {
        this.selectedSkills = [];
        this.updatePotentialSkills();
      },
      onPrerequisiteSelected(item) {
        SkillsService.assignDependency(this.projectId, this.skillId, item.skillId, item.projectId).then(() => {
          this.loadDataForSkill();
          this.$emit('update');
        });
      },
      onPrerequisiteDeselected(item) {
        SkillsService.removeDependency(this.projectId, this.skillId, item.skillId, item.projectId).then(() => {
          this.loadDataForSkill();
          this.$emit('update');
        });
      },
      loadDataForSkill() {
        SkillsService.getDependentSkillsGraphForSkill(this.projectId, this.skillId).then((data) => {
          const mySkill = data.nodes.find((entry) => entry.skillId === this.skillId && entry.projectId === this.projectId);
          const myEdges = data.edges.filter((entry) => entry.fromId === mySkill.id);
          const myChildren = data.nodes.filter((item) => myEdges.find((item1) => item1.toId === item.id));

          this.selectedPrerequisites = myChildren.map((entry) => {
            const externalProject = entry.projectId !== this.projectId;
            return Object.assign(entry, {
              entryId: `${entry.projectId}_${entry.skillId}`,
              isFromAnotherProject: externalProject,
            });
          });
          this.updatePotentialSkills();
        });
      },
    },
  };
</script>

<style>

</style>
