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
  <div id="project-selector" data-cy="projectSelector">
    <v-select v-model="selectedValue"
              :options="projects"
              :multiple="!onlySingleSelectedValue"
              :filterable="false"
              placeholder="Search for a project..."
              label="name"
              v-on:search="search"
              v-on:input="inputChanged"
              :loading="isLoading"
              :disabled="disabled">
      <template #option="{ name, projectId }">
        <h6>{{ name }}</h6>
        <div class="text-secondary">ID: {{ projectId }}</div>
      </template>
    </v-select>
  </div>
</template>

<script>
  import vSelect from 'vue-select';
  import ProjectService from '../../projects/ProjectService';

  export default {
    name: 'ProjectSelector',
    components: { vSelect },
    props: {
      projectId: String,
      selected: Object,
      disabled: {
        type: Boolean,
        default: false,
      },
      onlySingleSelectedValue: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        isLoading: false,
        projects: [],
        selectedValue: null,
      };
    },
    mounted() {
      this.selectedValue = this.selected;
      this.search('');
    },
    watch: {
      selected: function selectionChanged() {
        this.selectedValue = this.selected;
      },
    },
    methods: {
      onSelected(selectedItem) {
        this.$emit('selected', selectedItem);
      },
      onRemoved(item) {
        this.$emit('unselected', item);
      },
      inputChanged(inputItem) {
        if (inputItem != null) {
          this.onSelected(inputItem);
        } else {
          this.onRemoved(null);
        }
      },
      search(query) {
        this.isLoading = true;
        ProjectService.queryOtherProjectsByName(this.projectId, query)
          .then((response) => {
            this.isLoading = false;
            this.projects = response;
          });
      },
    },
  };
</script>

<style>
  #project-selector .multiselect__tag {
    background-color: lightblue;
    color: black;
  }
</style>
