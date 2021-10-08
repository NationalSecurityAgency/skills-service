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
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
    <b-modal :id="projectId" size="md" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             header-text-variant="light" no-fade>
      <b-container fluid>
          <label for="existingLevel">Existing level</label>
          <b-form-input id="existingLevel" :readonly="true" v-model="oldLevel" />
          <div class="mt-3" aria-hidden="true"/>
          <label for="newLevel">New level</label>
          <ValidationProvider name="New Level" :debounce=500 v-slot="{errors}" rules="required|min_value:0|max_value:100">
            <!-- this stupid thing doesn't load on mount -->
            <level-selector v-model="newLevel"
                            :load-immediately="true"
                            id="newLevel"
                            :project-id="projectId"
                            placeholder="select new project level"></level-selector>
            <small class="form-text text-danger" v-show="errors[0]" data-cy="newLevelError" id="newLevelError">{{ errors[0] }}</small>
          </ValidationProvider>
      </b-container>
      <div slot="modal-footer" class="w-100">
        <b-button variant="success"
                  size="sm"
                  class="float-right"
                  @click="handleSubmit(saveLevelChange)"
                  :disabled="invalid || newLevel === oldLevel"
                  v-skills="'AddOrModifyLevels'"
                  data-cy="saveLevelButton">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe" data-cy="cancelLevel">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import LevelSelector from '@/components/levels/global/LevelSelector';

  export default {
    name: '',
    props: {
      projectId: {
        type: String,
        required: true,
      },
      currentLevel: {
        type: Number,
        required: true,
      },
      title: {
        type: String,
        required: false,
        default: '',
      },
    },
    components: { LevelSelector },
    data() {
      return {
        show: true,
        newLevel: null,
        oldLevel: this.currentLevel,
      };
    },
    methods: {
      saveLevelChange() {
        this.$emit('level-changed', {
          projectId: this.projectId,
          oldLevel: this.currentLevel,
          newLevel: this.newLevel,
        });
        this.closeMe();
      },
      closeMe() {
        this.show = false;
        this.publishHidden();
      },
      publishHidden() {
        this.$emit('hidden', { projectId: this.projectId });
      },
    },
  };
</script>

<style scoped>
</style>
