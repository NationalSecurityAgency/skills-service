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
  <div id="skills-selector" class="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" :placeholder="placeholder" :select-label="selectLabel"
                 :options="optionsInternal" :multiple="multipleSelection" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="entryId" :is-loading="isLoading"
                 v-on:select="added" v-on:search-change="searchChanged" :internal-search="internalSearch"
                 data-cy="skillsSelector">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <div class="h5 text-info" data-cy="skillsSelector-skillName">{{ props.option.name }}</div>
          <div class="" style="font-size: 0.8rem;">
            <span>
              <span v-if="showProject"><span class="text-uppercase mr-1 font-italic">Project ID:</span><span class="font-weight-bold" data-cy="skillsSelector-projectId">{{props.option.projectId}}</span></span>
              <span v-if="!showProject"><span class="text-uppercase mr-1 font-italic">ID:</span><span class="font-weight-bold" data-cy="skillsSelector-skillId">{{props.option.skillId}}</span></span>
            </span>
            <span class="mx-2">|</span>
            <span class="text-uppercase mr-1 font-italic">Subject:</span><span class="font-weight-bold" data-cy="skillsSelector-subjectName">{{props.option.subjectName}}</span>
          </div>
        </slot>
      </template>
      <template slot="tag" slot-scope="{ option, remove }">
        <span class="selected-tag mr-2 border rounded p-1">
          <span>{{ option.name }}</span>
          <span class="border rounded ml-1 remove-x" v-on:click.stop="considerRemoval(option, remove)">❌</span>
        </span>
      </template>
      <template v-if="afterListSlotText" slot="afterList">
        <h6 class="ml-1"> {{ this.afterListSlotText }}</h6>
      </template>
      <template slot="noOptions">
        <span v-if="emptyWithoutSearch && !internalSearch"><i class="fas fa-search"/> Type to <span class="font-weight-bold">search</span> for skills...</span>
        <span v-else>List is empty!</span>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'SkillsSelector2',
    components: { Multiselect },
    mixins: [MsgBoxMixin],
    props: {
      options: {
        type: Array,
        required: true,
      },
      selected: {
        type: Array,
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
        default: 'Select skill(s)...',
      },
      selectLabel: {
        type: String,
        default: 'Press enter to select',
      },
    },
    data() {
      return {
        selectedInternal: [],
        optionsInternal: [],
        multipleSelection: true,
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
          this.selectedInternal = this.selected.map((entry) => ({ entryId: `${entry.projectId}_${entry.skillId}`, ...entry }));
        }
      },
      setOptionsInternal() {
        if (this.options) {
          this.optionsInternal = this.options.map((entry) => ({ entryId: `${entry.projectId}_${entry.skillId}`, ...entry }));
        }
      },
      considerRemoval(removedItem, removeMethod) {
        const msg = `Are you sure you want to remove "${removedItem.name}"?`;
        this.msgConfirm(msg, 'WARNING', 'Yes, Please!').then((res) => {
          if (res) {
            this.removed(removedItem, removeMethod);
          }
        });
      },
      removed(removedItem, removeMethod) {
        removeMethod(removedItem);
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('added', addedItem);
      },
      searchChanged(query) {
        this.$emit('search-change', query);
      },
    },
  };
</script>

<style scoped>

</style>

<style>
</style>
