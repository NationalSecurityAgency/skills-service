<template>
  <div id="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" placeholder="Select skill(s)..."
                 :options="options" :multiple="multipleSelection" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="id" v-on:remove="removed" v-on:select="added">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <h6>{{ props.option.name }}</h6>
          <div class="text-secondary">ID: {{props.option.skillId}}</div>
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
