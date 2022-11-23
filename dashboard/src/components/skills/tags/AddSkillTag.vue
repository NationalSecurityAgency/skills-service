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
    <b-modal :id="projectId" size="md" title="Tag Selected Skills" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="cancel"
             header-text-variant="light" no-fade
             role="dialog"
             aria-label="'Tag Skills in this project'">
      <b-container fluid>
        <label for="existingTag">Select Existing Tag</label>
        <div id="existingTag">
          <v-select v-model="existingTagValue" :options="existingTags" label="tagValue"
                    placeholder="Select Tag" v-on:input="existingTagInputChanged" :loading="isLoading">
          </v-select>
        </div>
        <div class="mt-3" aria-hidden="true"/>
        <ValidationProvider name="Skill Tags" :debounce=500 v-slot="{errors}" rules="maxSkillTagLength">
          <label for="newTag">Create New Tag</label>
          <b-form-input id="newTag" v-model="newTagValue"
                        @input="newTagInputChanged"
                        @keydown.enter="handleSubmit(tagSkills)"
          />
          <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="newTagError" id="newTagError">{{ errors[0] }}</small>
        </ValidationProvider>
      </b-container>
      <div slot="modal-footer" class="w-100">
        <b-button variant="success"
                  size="sm"
                  class="float-right"
                  @click="handleSubmit(tagSkills)"
                  :disabled="invalid || !tagValue"
                  v-skills="'AddOrModifyTags'"
                  data-cy="addTagsButton">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="cancel" data-cy="cancelAddTagsButton">
          Cancel
        </b-button>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import vSelect from 'vue-select';
  import SkillsService from '@/components/skills/SkillsService';
  import InputSanitizer from '../../utils/InputSanitizer';

  export default {
    name: '',
    props: {
      skills: {
        type: Array,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    components: { vSelect },
    data() {
      return {
        isLoading: false,
        show: this.value,
        newTagValue: null,
        existingTagValue: null,
        tagId: null,
        tagValue: null,
        existingTags: [],
      };
    },
    mounted() {
      this.loadExistingTags();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      projectId() {
        return this.$route.params.projectId;
      },
    },
    methods: {
      tagSkills() {
        if (this.tagId && this.tagValue) {
          const skillIds = this.skills.map((skill) => skill.skillId);
          const tag = { tagId: this.tagId, tagValue: this.tagValue };
          SkillsService.addTagToSkills(this.$route.params.projectId, skillIds, this.tagId, this.tagValue)
            .then(() => {
              this.$emit('action-success', {
                skills: this.skills,
                tag,
              });
              this.show = false;
            });
        }
      },
      cancel(e) {
        this.show = false;
        this.publishHidden(e, true);
      },
      publishHidden(e, cancelled) {
        this.$emit('hidden', {
          ...e,
          cancelled,
        });
      },
      loadExistingTags() {
        this.isLoading = true;
        SkillsService.getTagsForProject(this.projectId)
          .then((res) => {
            this.existingTags = res;
          })
          .finally(() => {
            this.isLoading = false;
          });
        // const tags = ['Beginner', 'Novice', 'Intermediate', 'Advanced', 'Expert'];
        // return tags;
      },
      existingTagInputChanged(input) {
        this.newTagValue = null;
        this.tagId = input?.tagId;
        this.tagValue = input?.tagValue;
      },
      newTagInputChanged(input) {
        this.existingTagValue = null;
        this.tagValue = input?.trim();
        this.tagId = InputSanitizer.removeSpecialChars(input)?.toLowerCase();
      },
    },
  };
</script>

<style scoped>
</style>
