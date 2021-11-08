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
  <b-modal :id="`edtRequiredNumSkillsModal-${group.skillId}`"
           title="Required Number of Skills"
           v-model="show"
           @hide="publishHidden"
           :no-close-on-backdrop="true"
           :centered="true"
           data-cy="EditSkillGroupModal"
           header-bg-variant="info"
           ok-title="Save"
           @ok="handleSave"
           :cancel-disabled="loading"
           :ok-disabled="loading || skillsPointsSettingsDoNotMatch"
           header-text-variant="light" no-fade>
    <b-overlay :show="loading" rounded="sm" opacity="0.4" spinner-variant="info">
    <div v-if="skillsPointsSettingsDoNotMatch" data-cy="syncSkillsPointsSection">
        <div>
          <i class="fas fa-exclamation-circle text-warning"></i> Group's skills points <b>must</b> be the same. Set all skills to:
          <div class="row mt-3">
            <div class="col">
              <div class="form-group mb-1">
                <label>Increment</label>
                <b-form-input type="number" v-model="syncSkillsPoints.pointIncrement" data-cy="pointIncrement"></b-form-input>
              </div>
            </div>
            <div class="col">
              <div class="form-group mb-1">
                <label>Occurrences</label>
                <div class="input-group">
                  <div class="input-group-prepend">
                    <div class="input-group-text"><i class="fas fa-times"></i></div>
                  </div>
                  <b-form-input type="number" v-model="syncSkillsPoints.numPerformToCompletion" data-cy="numPerformToCompletion"></b-form-input>
                </div>
              </div>
            </div>
            <div class="col">
              <div class="form-group mb-1">
                <label>Total Points</label>
                <div class="input-group">
                  <div class="input-group-prepend">
                    <div class="input-group-text"><i class="fas fa-equals"/></div>
                  </div>
                  <div class="form-control font-italic" style="background: #eeeeee;" data-cy="totalPoints">{{ totalSyncPoints }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="text-right mt-2">
          <b-button variant="outline-success" data-cy="syncBtn" :disabled="totalSyncPoints <= 0"
            @click="syncSkills"><i class="fas fa-sync"></i> Sync Group's Points</b-button>
        </div>
        <hr />
    </div>
    <b-form inline :data-cy="`editRequiredModal-${group.skillId}`">
      <span class="mr-1 text-secondary">Required: </span>
      <b-form-select size="sm" v-model="numSkillsRequired.selected" :options="numSkillsRequired.options" :disabled="skillsPointsSettingsDoNotMatch"
                     data-cy="requiredSkillsNumSelect"/>
      <span class="ml-1">skills</span>
      <div v-b-tooltip.hover.v-info class="ml-1 text-warning"
        title="A Group allows a Skill to be defined by the collection of other Skills within a Project. A Skill Group can require the completion of some or all of the included Skills before the group be achieved.">
        <i class="fas fa-question-circle"></i>
      </div>
    </b-form>
    </b-overlay>
  </b-modal>

</template>

<script>
  import SkillsService from '../SkillsService';

  export default {
    name: 'EditNumRequiredSkills',
    props: {
      group: Object,
      skills: Array,
      value: {
        type: Boolean,
        required: true,
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    data() {
      return {
        show: this.value,
        numSkillsRequired: {
          options: [],
          selected: null,
        },
        syncSkillsPoints: {
          numPerformToCompletion: 5,
          pointIncrement: 10,
        },
        loading: false,
      };
    },
    mounted() {
      this.updateNumSkillsRequired();
      this.syncSkillsPoints.pointIncrement = this.skills[0].pointIncrement;
      this.syncSkillsPoints.numPerformToCompletion = this.skills[0].numPerformToCompletion;
    },
    computed: {
      skillsPointsSettingsDoNotMatch() {
        const first = this.skills[0];
        const diffSkill = this.skills.find((skill) => skill.numPerformToCompletion !== first.numPerformToCompletion || skill.pointIncrement !== first.pointIncrement);
        return diffSkill !== undefined && diffSkill !== null;
      },
      totalSyncPoints() {
        return this.syncSkillsPoints.numPerformToCompletion * this.syncSkillsPoints.pointIncrement;
      },
    },
    methods: {
      publishHidden(e) {
        this.$emit('hidden', e);
      },
      updateNumSkillsRequired() {
        const numSkills = this.skills.length;
        const options = [];
        for (let i = 1; i < numSkills; i += 1) {
          options.push({ value: i, text: `${i} out of ${numSkills}` });
        }
        options.push({ value: -1, text: 'ALL SKILLS' });
        this.numSkillsRequired.options = options;
        this.numSkillsRequired.selected = this.group.numSkillsRequired;
      },
      handleSave() {
        if (this.numSkillsRequired.selected < this.skills.length) {
          const updatedGroup = {
            ...this.group,
            numSkillsRequired: this.numSkillsRequired.selected,
          };
          this.$emit('group-changed', updatedGroup);
        }
      },
      syncSkills() {
        this.loading = true;
        SkillsService.syncSkillsPoints(this.group.projectId, this.group.subjectId, this.group.skillId,
                                       {
                                         pointIncrement: this.syncSkillsPoints.pointIncrement,
                                         numPerformToCompletion: this.syncSkillsPoints.numPerformToCompletion,
                                       })
          .then((res) => {
            this.$emit('skills-updated', res);
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
