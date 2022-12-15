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
    <b-modal :id="testInternal.testId" size="xl" :title="title" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="publishHidden"
             header-text-variant="light" no-fade>
      <b-container fluid>
        <div class="form-group">
          <label for="projectIdInput">* Name</label>
          <ValidationProvider
            rules="required|minNameLength|maxTestNameLength|uniqueName|customNameValidator"
            v-slot="{errors}"
            name="Test Name">
            <input class="form-control" type="text" v-model="testInternal.name"
                   v-on:input="updateTestd"
                   v-on:keydown.enter="handleSubmit(updateProject)"
                   v-focus
                   data-cy="testName"
                   id="testNameInput"
                   :aria-invalid="errors && errors.length > 0"
                   aria-errormessage="testNameError"
                   aria-describedby="testNameError"/>
            <small role="alert" class="form-text text-danger" data-cy="testNameError"
                   id="testNameError">{{ errors[0] }}</small>
          </ValidationProvider>
        </div>

        <id-input type="text" label="Test ID" v-model="testInternal.testId"
                  additional-validation-rules="uniqueId"
                  v-on:keydown.enter.native="handleSubmit(updateProject)"
                  :next-focus-el="previousFocus"
                  @shown="tooltipShowing=true"
                  @hidden="tooltipShowing=false"/>

        <label>Description</label>
        <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{errors}"
                            name="Badge Description">
          <markdown-editor v-model="testInternal.description"></markdown-editor>
          <small role="alert" class="form-text text-danger mb-3" data-cy="badgeDescriptionError">{{ errors[0] }}</small>
        </ValidationProvider>

        <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite">
          <small>***{{ overallErrMsg }}***</small></p>
      </b-container>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import InputSanitizer from '@/components/utils/InputSanitizer';
  import IdInput from '@/components/utils/inputForm/IdInput';
  import MarkdownEditor from '@/components/utils/MarkdownEditor';

  export default {
    name: 'EditTest',
    components: { MarkdownEditor, IdInput },
    props: {
      test: Object,
      isEdit: {
        type: Boolean,
        default: false,
      },
      value: Boolean,
    },
    data() {
      const testInternal = {
        originalBadgeId: this.test.testId,
        isEdit: this.isEdit,
        ...this.test,
      };
      return {
        show: this.value,
        testInternal,
        currentFocus: null,
        previousFocus: null,
        tooltipShowing: false,
        overallErrMsg: '',
      };
    },
    mounted() {
      document.addEventListener('focusin', this.trackFocus);
      if (this.isEdit) {
        setTimeout(() => {
          this.$nextTick(() => {
            const { observer } = this.$refs;
            if (observer) {
              observer.validate({ silent: false });
            }
          });
        }, 600);
      }
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Test' : 'New Test';
      },
    },
    methods: {
      trackFocus() {
        this.previousFocus = this.currentFocus;
        this.currentFocus = document.activeElement;
      },
      closeMe(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        if (this.tooltipShowing) {
          e.preventDefault();
        } else {
          this.$emit('hidden', e);
        }
      },
      updateTestd() {
        if (!this.isEdit) {
          this.testInternal.testId = InputSanitizer.removeSpecialChars(this.testInternal.name);
        }
      },
    },
  };
</script>

<style scoped>

</style>
