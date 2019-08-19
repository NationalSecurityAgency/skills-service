<template>
  <div id="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" placeholder="Select skill(s)..."
                 :options="options" :multiple="multipleSelection" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="id" :is-loading="isLoading"
                 v-on:remove="removed" v-on:select="added" v-on:search-change="searchChanged" :internal-search="internalSearch">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <h6>{{ props.option.name }}</h6>
          <div class="text-secondary">ID: {{props.option.skillId}}</div>
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

  export default {
    name: 'SkillsSelector2',
    components: { Multiselect },
    props: {
      options: {
        type: Array,
        required: true,
      },
      selected: {
        type: Array,
      },
      onlySingleSelectedValue: {
        type: Boolean,
        default: false,
      },
      isLoading: {
        type: Boolean,
        default: false,
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
        selectedInternal: null,
        multipleSelection: true,
      };
    },
    mounted() {
      this.setSelectedInternal();
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
        if (this.selected) {
          this.selectedInternal = this.selected.map(entry => entry);
        }
      },
      removed(removedItem) {
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('added', addedItem);
      },
      searchChanged(query) {
        this.$emit('search-change', query);
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
