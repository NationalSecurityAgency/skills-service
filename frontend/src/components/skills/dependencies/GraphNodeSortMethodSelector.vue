<template>
  <b-field>
    <b-radio-button v-model="sortMethod" native-value="directed">
      <b-tooltip label="Directed algorithm adheres to the to and from data of the edges. A --> B so B is a level lower than A."
                 position="is-bottom" animanted="true" type="is-info" multilined>
        <span><i class="fas fa-vector-square"></i> Directed</span>
      </b-tooltip>

    </b-radio-button>

    <b-radio-button v-model="sortMethod" native-value="hubsize">
      <b-tooltip label="Hubsize algorithm takes the nodes with the most edges and puts them at the top. From that the rest of the hierarchy is evaluated."
                 position="is-bottom" animanted="true" type="is-info" multilined>
        <span><i class="fas fa-bezier-curve"></i> Hubsize</span>
      </b-tooltip>
    </b-radio-button>
  </b-field>
</template>

<script>
  export default {
    name: 'GraphNodeSortMethodSelector',
    data() {
      return {
        sortMethod: 'directed',
      };
    },
    watch: {
      sortMethod: function watchUpdatesToSelected() {
        this.notifyOfChange();
      },
    },
    methods: {
      notifyOfChange() {
        this.$emit('value-changed', this.sortMethod);
      },
    },
  };
</script>

<style scoped>

</style>
