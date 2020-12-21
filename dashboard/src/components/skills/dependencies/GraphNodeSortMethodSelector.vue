/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div>
      <b-form-radio-group id="radio-group-2" v-model="sortMethod"
                          name="graph-sort-method" button-variant="outline-hc" stacked buttons>
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
