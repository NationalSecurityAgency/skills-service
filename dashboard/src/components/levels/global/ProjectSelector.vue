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
    <v-select :options="projects"
              placeholder="Select Project..."
              :filterable="internalSearch"
              label="name"
              v-on:search="searchChanged"
              v-on:input="inputChanged"
              :loading="isLoading">
      <template #option="{ name, projectId }">
        <h6>{{ name }}</h6>
        <div class="text-secondary">ID: {{ projectId }}</div>
      </template>
      <template v-if="afterListSlotText" #list-footer>
        <li>
          <h6 class="ml-1"> {{ afterListSlotText }}</h6>
        </li>
      </template>
    </v-select>
  </div>
</template>

<script>
  import vSelect from 'vue-select';
  import GlobalBadgeService from '../../badges/global/GlobalBadgeService';

  export default {
    name: 'ProjectSelector',
    components: { vSelect },
    props: {
      value: {
        type: Object,
      },
      internalSearch: {
        type: Boolean,
        default: true,
      },
      afterListSlotText: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        isLoading: false,
        projects: [],
        selectedInternal: null,
        badgeId: null,
      };
    },
    mounted() {
      this.badgeId = this.$route.params.badgeId;
      this.isLoading = true;
      this.setSelectedInternal();
      this.loadProjectsForBadge();
    },
    watch: {
      value: function watchUpdatesToSelected() {
        this.setSelectedInternal();
      },
    },
    methods: {
      setSelectedInternal() {
        if (this.value) {
          this.selectedInternal = { ...this.value };
        } else {
          this.selectedInternal = null;
        }
      },
      removed(removedItem) {
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('input', addedItem);
        this.$emit('added', addedItem);
      },
      inputChanged(inputItem) {
        if (inputItem != null) {
          this.added(inputItem);
        } else {
          this.removed(null);
        }
      },
      searchChanged(query, loadingFunction) {
        this.$emit('search-change', query, loadingFunction);
      },
      loadProjectsForBadge() {
        GlobalBadgeService.getAllProjectsForBadge(this.badgeId)
          .then((response) => {
            this.isLoading = false;
            this.projects = response.map((entry) => entry);
          });
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
  }
</style>
