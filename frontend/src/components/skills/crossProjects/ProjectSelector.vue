<template>
  <div id="project-selector">


    <!--<multiselect v-model="selectedInternal" placeholder="Select skill(s)..."-->
                 <!--:options="options" :multiple="true" :taggable="multipleSelection"-->
                 <!--:hide-selected="true" label="name" track-by="id" v-on:remove="removed" v-on:select="added">-->

    <multiselect v-model="selectedValue" placeholder="Type to search for a project..."
                 :options="projects" :multiple="true" :taggable="false"
                 :hide-selected="true" label="name" track-by="id" v-on:select="onSelected" v-on:remove="onRemoved"
                 @search-change="search" :loading="isLoading" :internal-search="false"
                 :clear-on-select="false">
      <template slot="option" slot-scope="props">
        <div class="columns">
          <div class="column handle-overflow" style="flex:none; width:45%;" :title="props.option.name"><span class="selector-skill-name">{{ props.option.name }}</span></div>
          <div class="column is-one-fifth handle-overflow" style="flex:none; width:30%;" :title="props.option.projectId">
            <span class="selector-other-label">ID:</span> <span class="selector-other-value">{{props.option.projectId}}</span>
          </div>
        </div>
      </template>

      <!--<template slot="noResult">-->
        <!--dljalj lajlkdjalj dlkajl-->
      <!--</template>-->
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import ProjectService from '../../projects/ProjectService';

  export default {
    name: 'ProjectSelector',
    components: { Multiselect },
    props: ['projectId', 'selected'],
    data() {
      return {
        isLoading: false,
        projects: [],
        selectedValue: null,
      };
    },
    mounted() {
      this.selectedValue = this.selected;
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
            this.projects = response.filter(entry => entry.projectId !== this.projectId);
        });
      },
    },
  };
</script>

<style>
  #project-selector .multiselect__tag {
    background-color: lightblue;
    color: black;
    /*margin: 10px;*/
  }
</style>
