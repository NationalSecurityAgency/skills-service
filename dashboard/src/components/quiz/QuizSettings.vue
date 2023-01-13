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
  <sub-page-header title="Settings"/>

  <b-card>
    <ValidationObserver ref="observer" v-slot="{ invalid, handleSubmit }" slim>
      <div class="row">
        <div id="quizTypeLabel" class="col col-md-3 text-secondary" >
          Quiz Type:
        </div>
        <div class="col">
          <b-form-select v-model="settings.quizType.value"
                         :options="options.quizType"
                         @input="quizTypeChanged"
                         aria-labelledby="quizTypeLabel"
                         data-cy="quizTypeSelector" required/>
        </div>
      </div>

      <hr/>

      <div class="row">
        <div class="col">
          <b-button variant="outline-success" @click="handleSubmit(save)" :disabled="invalid || !isDirty" data-cy="saveSettingsBtn">
            Save <i class="fas fa-arrow-circle-right"/>
          </b-button>

          <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
                  <i class="fa fa-exclamation-circle"
                     aria-label="Settings have been changed, do not forget to save"
                     v-b-tooltip.hover="'Settings have been changed, do not forget to save'"/> Unsaved Changes
                </span>
          <span v-if="!isDirty && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
                  <i class="fa fa-check" />
                  Settings Updated!
                </span>
        </div>
      </div>
    </ValidationObserver>
  </b-card>
</div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';

  export default {
    name: 'QuizSettings',
    components: { SubPageHeader },
    data() {
      return {
        isLoading: true,
        showSavedMsg: false,
        settings: {
          quizType: {
            value: 'Quiz',
            setting: 'quizType',
            lastLoadedValue: 'Quiz',
            dirty: false,
          },
        },
        options: {
          quizType: [
            { value: 'Quiz', text: 'Quiz' },
            { value: 'Survey', text: 'Survey' },
          ],
        },
      };
    },
    computed: {
      isDirty() {
        const foundDirty = Object.values(this.settings).find((item) => item.dirty);
        return !!foundDirty;
      },
    },
    methods: {
      quizTypeChanged(value) {
        this.settings.quizType.dirty = `${value}` !== `${this.settings.quizType.lastLoadedValue}`;
      },
    },
  };
</script>

<style scoped>

</style>
