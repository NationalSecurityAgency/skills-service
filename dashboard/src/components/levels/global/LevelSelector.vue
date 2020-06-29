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
  <div id="level-selector">

    <multiselect :disabled="disabled" v-model="selectedInternal" :options="projectLevels"
                 :show-labels="false" :hide-selected="true" :is-loading="isLoading"
                 :placeholder="placeholder" v-on:select="selected"></multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import GlobalBadgeService from '../../badges/global/GlobalBadgeService';

  export default {
    name: 'LevelSelector',
    components: { Multiselect },
    props: {
      value: {
        type: Number,
      },
      projectId: {
        type: String,
      },
      disabled: {
        type: Boolean,
      },
      placeholder: {
        type: String,
      },
    },
    data() {
      return {
        isLoading: false,
        projectLevels: [],
        selectedInternal: null,
      };
    },
    mounted() {
      this.setSelectedInternal();
    },
    watch: {
      projectId: function watchUpdatesToProjectId(newProjectId) {
        this.selectedInternal = null;
        if (!newProjectId) {
          this.projectLevels = [];
        } else {
          this.loadProjectLevels(newProjectId);
        }
      },
    },
    methods: {
      loadProjectLevels(projectId) {
        this.isLoading = true;
        GlobalBadgeService.getProjectLevels(projectId)
          .then((response) => {
            this.projectLevels = response.map(entry => entry.level);
          }).finally(() => {
            this.isLoading = false;
          });
      },
      setSelectedInternal() {
        if (this.value) {
          this.selectedInternal = this.value;
        }
      },
      removed(removedItem) {
        this.$emit('removed', removedItem);
      },
      selected(selectedItem) {
        this.$emit('input', selectedItem);
      },
    },
  };
</script>

<style>
  #skills-selector .multiselect{
    z-index: 99;
  }

  #skills-selector .multiselect__tag {
    background-color: lightblue;
    color: black;
    /*margin: 10px;*/
  }
</style>
