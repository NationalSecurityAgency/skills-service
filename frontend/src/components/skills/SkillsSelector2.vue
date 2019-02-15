<template>
  <div id="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" placeholder="Select skill(s)..."
                 :options="options" :multiple="multipleSelection" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="id" v-on:remove="removed" v-on:select="added">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <div class="columns">
            <div class="column skills-handle-overflow" style="flex:none; width:45%;" :title="props.option.name"><span class="selector-skill-name">{{ props.option.name }}</span></div>
            <div class="column is-one-fifth skills-handle-overflow" style="flex:none; width:30%;" :title="props.option.skillId">
              <span class="selector-other-label">ID:</span> <span class="selector-other-value">{{props.option.skillId}}</span>
            </div>
            <div class="column is-one-fifth" style="flex:none; width:20%;">
              <span class="selector-other-label">Total Points:</span> <span class="selector-other-value">{{ props.option.totalPoints}}</span>
            </div>
          </div>
        </slot>
      </template>
      <span slot="tag" slot-scope="props">
        <slot name="selected-item" v-bind:props="props">
          <span class="tag selected-item" style="margin-right: 7px;">
          <span class="skills-handle-overflow selected-item" style="width: 8rem;" :title="props.option.name">{{ props.option.name }}</span>
            <button class="delete is-small" v-on:click="props.remove(props.option)"></button>
          </span>
        </slot>
      </span>
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

  #skills-selector .selected-item {
    background-color: lightblue;
    color: black;
  }

</style>
