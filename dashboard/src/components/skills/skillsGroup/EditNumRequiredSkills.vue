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
           header-text-variant="light" no-fade>
<!--    <div>-->
<!--&lt;!&ndash;      <b-badge variant="success">COMPLIANT</b-badge>&ndash;&gt;-->
<!--      <i class="fas fa-check-double fa-2x text-success"></i>-->
<!--      All skills have the same # of points .-->
<!--      <span class="text-secondary">(Please note that this operation is only allowed if all the skills in the group have same amount of points.)</span>-->
<!--    </div>-->
<!--    <hr />-->

        <div>
    <!--      <b-badge variant="success">COMPLIANT</b-badge>-->
          <i class="fas fa-exclamation-circle text-warning"></i> Group's skills points <b>must</b> be the same. Set all skills to:
          <div class="row mt-3">
            <div class="col">
              <div class="form-group mb-1">
                <label>Increment</label>
                <b-form-input :id="`type`" type="number" value="10"></b-form-input>
              </div>
            </div>
            <div class="col">
              <div class="form-group mb-1">
                <label>Occurrences</label>
                <div class="input-group">
                  <div class="input-group-prepend">
                    <div class="input-group-text"><i class="fas fa-times"></i></div>
                  </div>
                  <div class="form-control font-italic">5</div>
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
                  <div class="form-control font-italic" style="background: #eeeeee;">50</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="text-right mt-2">
          <b-button variant="outline-success"><i class="fas fa-sync"></i> Sync Group's Points</b-button>
        </div>
        <hr />
    <b-form inline>
      <span class="mr-1 text-secondary">Required: </span>
      <b-form-select size="sm" v-model="numSkillsRequired.selected" :options="numSkillsRequired.options" />
      <span class="ml-1">out <b-badge>{{ skills.length }}</b-badge> skills</span>
    </b-form>
  </b-modal>
</template>

<script>
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
      };
    },
    mounted() {
      this.updateNumSkillsRequired();
    },
    methods: {
      publishHidden(e) {
        this.$emit('hidden', e);
      },
      updateNumSkillsRequired() {
        this.numSkillsRequired.options = Array.from({ length: this.skills.length }, (_, i) => i + 1);
        this.numSkillsRequired.selected = this.numSkillsRequired.options[this.numSkillsRequired.options.length - 1];
      },
    },
  };
</script>

<style scoped>

</style>
