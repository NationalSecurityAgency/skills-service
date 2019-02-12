<template>
  <div id="skills-selector">

    <multiselect v-model="selectedInternal" placeholder="Select skill(s)..."
                 :options="options" :multiple="multipleSelection" :taggable="false"
                 :hide-selected="true" label="name" track-by="id" v-on:remove="removed" v-on:select="added">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <div class="columns">
            <div class="column handle-overflow" style="flex:none; width:45%;" :title="props.option.name"><span class="selector-skill-name">{{ props.option.name }}</span></div>
            <div class="column is-one-fifth handle-overflow" style="flex:none; width:30%;" :title="props.option.skillId">
              <span class="selector-other-label">ID:</span> <span class="selector-other-value">{{props.option.skillId}}</span>
            </div>
            <div class="column is-one-fifth" style="flex:none; width:20%;">
              <span class="selector-other-label">Total Points:</span> <span class="selector-other-value">{{ props.option.totalPoints}}</span>
            </div>
          </div>
        </slot>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';

  export default {
    name: 'SkillsSelector2',
    components: { Multiselect },
    props: ['options', 'selected', 'onlySingleSelectedValue'],
    data() {
      return {
        selectedInternal: null,
        multipleSelection: true,
      };
    },
    mounted() {
      this.selectedInternal = this.selected.map(entry => entry);
      if (this.onlySingleSelectedValue) {
        this.multipleSelection = false;
      }
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
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('added', addedItem);
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
