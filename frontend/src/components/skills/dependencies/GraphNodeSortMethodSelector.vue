<template>
  <div>
      <b-form-radio-group id="radio-group-2" v-model="sortMethod"
                          name="graph-sort-method" button-variant="outline-info" stacked buttons>
        <b-form-radio value="directed" v-b-tooltip.hover="msg.directed"><span><i class="fas fa-vector-square"></i> Directed</span></b-form-radio>
        <b-form-radio value="hubsize" v-b-tooltip.hover="msg.hubsize"><span><i class="fas fa-bezier-curve"></i> Hubsize</span></b-form-radio>
      </b-form-radio-group>
  </div>
</template>

<script>
  export default {
    name: 'GraphNodeSortMethodSelector',
    data() {
      return {
        sortMethod: 'directed',
        msg: {
          directed: 'Directed adheres to the to and from data of the edges. A --> B so B is a level lower than A.',
          hubsize: 'Hubsize takes the nodes with the most edges and puts them at the top. From that the rest of the hierarchy is evaluated.',
        },
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
