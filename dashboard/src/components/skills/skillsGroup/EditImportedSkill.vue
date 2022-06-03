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
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit }" slim>
    <b-modal :id="skill.skillId" size="md" title="Edit Catalog Imported Skill" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
             aria-label="Edit Imported Skill">

      <div class="alert alert-info">
        <i class="fas fa-book"></i> This skill was imported from <span class="text-info font-italic">{{ skill.copiedFromProjectName }}</span> and can only be modified in that project.
        You can change the <b>Point Increment</b> in order to scale the total points to your project's point layout.
      </div>

      <div class="form-group mb-1">
        <label for="pointIncrement">* Point Increment</label>
        <ValidationProvider rules="optionalNumeric|required|min_value:1|maxPointIncrement" v-slot="{errors}" name="Point Increment">
          <input class="form-control" type="text"  v-model="pointIncrement"
                 aria-required="true"
                 data-cy="skillPointIncrement"
                 v-on:keydown.enter="handleSubmit(saveSkill)"
                 id="pointIncrement"
                 aria-describedby="skillPointIncrementError"
                 aria-errormessage="skillPointIncrementError"
                 :aria-invalid="errors && errors.length > 0"/>
          <small class="form-text text-danger" data-cy="skillPointIncrementError" id="skillPointIncrementError">{{ errors[0] }}</small>
        </ValidationProvider>
      </div>

      <p v-if="invalid && overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>

      <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="handleSubmit(saveSkill)"
                  :disabled="invalid"
                  data-cy="saveSkillButton">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeSkillButton">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  export default {
    name: 'EditImportedSkill',
    props: {
      skill: {
        type: Object,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        pointIncrement: this.skill.pointIncrement,
        overallErrMsg: '',
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        this.$emit('hidden', { ...e });
      },
      saveSkill() {
        this.$refs.observer.validate()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.$emit('skill-saved', {
                ...this.skill,
                pointIncrement: this.pointIncrement,
                totalPoints: this.pointIncrement * this.skill.numPerformToCompletion,
              });
              this.close({ saved: true });
            }
          });
      },
    },
  };
</script>

<style scoped>

</style>
