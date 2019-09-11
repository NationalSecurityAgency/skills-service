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
