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
  <div id="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" placeholder="Select Project..."
                 :options="projects" :multiple="false" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="projectId" :is-loading="isLoading"
                 v-on:remove="removed" v-on:select="added" v-on:search-change="searchChanged" :internal-search="internalSearch">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <h6>{{ props.option.name }}</h6>
          <div class="text-secondary">ID: {{props.option.projectId}}</div>
        </slot>
      </template>
      <template v-if="afterListSlotText" slot="afterList">
        <h6 class="ml-1"> {{ this.afterListSlotText }}</h6>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import GlobalBadgeService from '../../badges/global/GlobalBadgeService';

  export default {
    name: 'ProjectSelector',
    components: { Multiselect },
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
          this.selectedInternal = Object.assign({}, this.value);
        } else {
          this.selectedInternal = null;
        }
      },
      removed(removedItem) {
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('added', addedItem);
        this.$emit('input', addedItem);
      },
      searchChanged(query) {
        this.$emit('search-change', query);
      },
      loadProjectsForBadge() {
        GlobalBadgeService.getAllProjectsForBadge(this.badgeId)
          .then((response) => {
            this.isLoading = false;
            this.projects = response.map(entry => entry);
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
    /*margin: 10px;*/
  }
</style>
