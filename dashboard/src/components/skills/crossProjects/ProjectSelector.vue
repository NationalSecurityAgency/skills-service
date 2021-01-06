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
  <div id="project-selector">
    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedValue" placeholder="Search for a project..."
                 :options="projects" :multiple="!onlySingleSelectedValue" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="id" v-on:select="onSelected" v-on:remove="onRemoved"
                 @search-change="search" :loading="isLoading" :internal-search="false"
                 :clear-on-select="false" :disabled="disabled">
      <template slot="option" slot-scope="props">
        <h6>{{ props.option.name }}</h6>
        <div class="text-secondary">ID: {{props.option.projectId}}</div>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import ProjectService from '../../projects/ProjectService';

  export default {
    name: 'ProjectSelector',
    components: { Multiselect },
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
