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
  <div class="container-fluid mt-2" >
    <h2 class="text-center">My Skills</h2>
    <b-row class="my-4">
      <b-col>
        <metrics-card title="card">
          1 of 3
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="card">
          2 of 3
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="card">
          2 of 3
        </metrics-card>
      </b-col>
    </b-row>

    <metrics-card title="Chart" class="mt-3">
      Chart
    </metrics-card>

    <metrics-card title="Chart" class="mt-3">
      Chart
    </metrics-card>

    <b-row class="my-4">
      <b-col>
        <metrics-card title="proj">
          Proj
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="proj">
          Proj
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="proj">
          Proj
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="proj">
          Proj
        </metrics-card>
      </b-col>
    </b-row>

  </div>
</template>

<script>
  import MetricsCard from '../metrics/utils/MetricsCard';

  export default {
    name: 'MySkillsPage',
    components: {
      MetricsCard,
    },
    data() {
      return {
        loading: true,
        projects: [],
      };
    },
    mounted() {
    },
    methods: {
    },
  };
</script>

<style scoped>

</style>
