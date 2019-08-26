<template>
  <div id="skills-selector">

    <!-- see https://github.com/shentao/vue-multiselect/issues/421 for explanation of :blockKeys-->
    <multiselect v-model="selectedInternal" placeholder="Select skill(s)..."
                 :options="optionsInternal" :multiple="multipleSelection" :taggable="false" :blockKeys="['Delete']"
                 :hide-selected="true" label="name" track-by="entryId" :is-loading="isLoading"
                 v-on:select="added" v-on:search-change="searchChanged" :internal-search="internalSearch">
      <template slot="option" slot-scope="props">
        <slot name="dropdown-item" v-bind:props="props">
          <h6>{{ props.option.name }}</h6>
          <div class="text-secondary">ID: {{props.option.skillId}}</div>
        </slot>
      </template>
      <template slot="tag" slot-scope="{ option, remove }">
        <span class="selected-tag mr-2 border rounded p-1">
          <span>{{ option.name }}</span>
          <span class="border rounded ml-1 remove-x" v-on:click.stop="considerRemoval(option, remove)">❌</span>
        </span>
      </template>
      <template v-if="afterListSlotText" slot="afterList">
        <h6 class="ml-1"> {{ this.afterListSlotText }}</h6>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'SkillsSelector2',
    components: { Multiselect },
    mixins: [MsgBoxMixin],
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
        selectedInternal: [],
        optionsInternal: [],
        multipleSelection: true,
      };
    },
    mounted() {
      this.setSelectedInternal();
      this.setOptionsInternal();
      if (this.onlySingleSelectedValue) {
        this.multipleSelection = false;
      }
    },
    watch: {
      selected: function watchUpdatesToSelected() {
        this.setSelectedInternal();
      },
      options: function watchUpdatesToOptions() {
        this.setOptionsInternal();
      },
    },
    methods: {
      setSelectedInternal() {
        if (this.selected) {
          this.selectedInternal = this.selected.map(entry => Object.assign({ entryId: `${entry.projectId}_${entry.skillId}` }, entry));
        }
      },
      setOptionsInternal() {
        if (this.options) {
          this.optionsInternal = this.options.map(entry => Object.assign({ entryId: `${entry.projectId}_${entry.skillId}` }, entry));
        }
      },
      considerRemoval(removedItem, removeMethod) {
        const msg = `Are you sure you want to remove "${removedItem.name}"?`;
        this.msgConfirm(msg, 'WARNING', 'Yes, Please!').then((res) => {
          if (res) {
            this.removed(removedItem, removeMethod);
          }
        });
      },
      removed(removedItem, removeMethod) {
        removeMethod(removedItem);
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

<style scoped>
  .selected-tag {
    background-color: lightblue;
    color: black;
  }
  .remove-x:hover {
    cursor: pointer;
  }
</style>
