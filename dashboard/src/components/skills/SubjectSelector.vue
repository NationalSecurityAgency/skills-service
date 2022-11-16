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
  <div class="skills-selector">
    <v-select :options="optionsInternal"
              v-model="selectedInternal"
              :placeholder="placeholder"
              :multiple="false"
              :filterable="internalSearch"
              label="name"
              v-on:search="searchChanged"
              v-on:option:selected="added"
              v-on:option:deselecting="removed"
              :loading="isLoading"
              :disabled="disabled"
              class="st-skills-selector"
              data-cy="subjectSelector">
      <template #option="option">
        <slot name="dropdown-item" :option="option">
          <div :data-cy="`subjectSelectionItem-${option.projectId}-${option.subjectId}`">
            <div class="h5 text-info skills-option-name" data-cy="subjSelector-name">{{ option.name }}
            </div>
            <div style="font-size: 0.8rem;">
              <span class="text-uppercase mr-1 font-italic"># Skills:</span>
              <span class="font-weight-bold" data-cy="skillsSelector-projectId">{{ option.numSkills}}</span>
              <span class="mx-2">|</span>
              <span class="text-uppercase mr-1 font-italic" data-cy="skillsSelectionItem-subjectId">Points:</span>
              <span class="font-weight-bold skills-option-subject-name" data-cy="skillsSelector-subjectName">{{ option.totalPoints | number }}</span>
            </div>
          </div>
        </slot>
      </template>
      <template #selected-option-container="{ option }">
        <div style="display: flex; align-items: baseline">
          <span class="selected-tag ml-2 mt-2 border rounded p-1">
            <span>{{ option.name }}</span>
            <span class="border rounded ml-1 remove-x"
                  :aria-label="`remove ${option.name} skill option button`"
                  tabindex="0"
                  v-on:keyup.enter="removed(option)"
                  v-on:click.stop="removed(option)">❌
            </span>
          </span>
        </div>
      </template>
      <template v-if="afterListSlotText" #list-footer>
        <li>
          <div class="h6 ml-1"> {{ afterListSlotText }}</div>
        </li>
      </template>
      <template #no-options>
        <span v-if="emptyWithoutSearch && !internalSearch && !currentSearch"><i class="fas fa-search"/> Type to <span class="font-weight-bold">search</span> for subjects...</span>
        <span v-else>No elements found. Consider changing the search query</span>
      </template>
    </v-select>
  </div>
</template>

<script>
  import vSelect from 'vue-select';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'SubjectSelector',
    components: { vSelect },
    mixins: [MsgBoxMixin],
    props: {
      options: {
        type: Array,
        required: true,
      },
      selected: {
        type: Object,
      },
      onlySingleSelectedValue: {
        type: Boolean,
        default: false,
      },
      isLoading: {
        type: Boolean,
        default: false,
      },
      internalSearch: {
        type: Boolean,
        default: true,
      },
      emptyWithoutSearch: {
        type: Boolean,
        default: false,
      },
      afterListSlotText: {
        type: String,
        default: '',
      },
      showProject: {
        type: Boolean,
        default: false,
      },
      placeholder: {
        type: String,
        default: 'Select subject',
      },
      placeholderIcon: {
        type: String,
        default: null,
      },
      selectLabel: {
        type: String,
        default: 'Press enter to select',
      },
      warnBeforeRemoving: {
        type: Boolean,
        default: true,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        selectedInternal: null,
        optionsInternal: [],
        multipleSelection: true,
        currentSearch: '',
      };
    },
    mounted() {
      this.setSelectedInternal();
      this.setOptionsInternal();
      if (this.onlySingleSelectedValue) {
        this.multipleSelection = false;
      }
    },
    watch: {
      selected: function watchUpdatesToSelected() {
        this.setSelectedInternal();
      },
      options: function watchUpdatesToOptions() {
        this.setOptionsInternal();
      },
    },
    methods: {
      setSelectedInternal() {
        if (this.selected) {
          this.selectedInternal = ({ ...this.selected });
        } else {
          this.selectedInternal = null;
        }
      },
      setOptionsInternal() {
        if (this.options) {
          this.optionsInternal = this.options.map((entry) => ({ entryId: `${entry.projectId}_${entry.subjectId}`, ...entry }));
          if (this.selected) {
            // removed already selected items
            this.optionsInternal = this.optionsInternal.filter((el) => !this.selected.some((sel) => `${sel.projectId}_${sel.subjectId}` === el.entryId));
          }
        }
      },
      removed(removedItem) {
        this.selectedInternal = null;
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        if (this.multipleSelection) {
          this.$emit('added', addedItem[addedItem.length - 1]);
        } else {
          this.$emit('added', addedItem);
        }
      },
      searchChanged(query, loadingFunction) {
        this.currentSearch = query;
        this.$emit('search-change', query, loadingFunction);
      },
    },
  };
</script>

<style scoped>
  .selected-tag {
    font-size: 14px;
  }

  >>> {
    --vs-line-height: 1.7;
  }

  .vs__dropdown-option--highlight .skills-option-id {
    color: #FFFFFF !important;
  }

  .vs__dropdown-option--highlight .skills-option-group-name {
    color: #FFFFFF !important;
  }

  .vs__dropdown-option--highlight .skills-option-subject-name {
    color: #FFFFFF !important;
  }
  .vs__dropdown-option--highlight .skills-option-name {
    color: #FFFFFF !important;
  }

  /*.skills-id :hover {
    color: #FFFFFF !important;
  }*/
</style>

<style>
</style>
