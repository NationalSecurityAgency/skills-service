<template>
  <div id="skills-selector">

    <!--<multiselect v-model="value" tag-placeholder="Add this as new tag" placeholder="Search or add a tag"-->
                 <!--label="name" track-by="code" :options="options" :multiple="true" :taggable="true" @tag="addTag"></multiselect>-->

    <multiselect v-model="selectedInternal" placeholder="Search to add a skill..."
                 :options="options" :multiple="true" :taggable="true"
                 :hide-selected="true" label="name" track-by="id" v-on:remove="removed" v-on:select="added">
      <template slot="option" slot-scope="props">
        <div class="columns">
          <div class="column handle-overflow" style="flex:none; width:45%;" :title="props.option.name"><span class="selector-skill-name">{{ props.option.name }}</span></div>
          <div class="column is-one-fifth handle-overflow" style="flex:none; width:30%;" :title="props.option.skillId">
            <span class="selector-other-label">ID:</span> <span class="selector-other-value">{{props.option.skillId}},</span>
          </div>
          <div class="column is-one-fifth" style="flex:none; width:20%;">
            <span class="selector-other-label">Total Points:</span> <span class="selector-other-value">{{ props.option.totalPoints}}</span>
          </div>
        </div>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';

  export default {
    name: 'SkillsSelector2',
    components: { Multiselect },
    props: ['options', 'selected'],
    data() {
      return {
        selectedInternal: null,
      };
    },
    mounted() {
      this.selectedInternal = this.selected.map(entry => entry);
    },
    watch: {
      selected: function watchUpdatesToSelected() {
        this.setSelectedInternal();
      },
    },
    methods: {
      setSelectedInternal() {
        this.selectedInternal = this.selected.map(entry => entry);
      },
      removed(removedItem) {
        // this.$toast.open(`Removed! removedOption=${removedOption.name}, id=${id}`);
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        // this.$toast.open(`Added! selectedOption=${selectedOption}, id=${id}`);
        this.$emit('added', addedItem);
      },
    },
  };
</script>

<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
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
