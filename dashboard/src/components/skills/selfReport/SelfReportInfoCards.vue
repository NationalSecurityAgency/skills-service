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
  <div class="row">
    <div class="col" v-for="card in cards" :key="card.label">
      <div class="card h-100" >
        <div class="card-body">
          <div class="row">
            <div class="col text-left">
              <div class="h5 card-title text-uppercase text-muted mb-0 small">{{card.label}}</div>
              <span class="h5 font-weight-bold mb-0" :data-cy="`selfReportInfoCardCount_${card.id}`">{{ card.count | number}}</span> skills
            </div>
            <div class="col-auto text-secondary">
              <i :class="card.icon" style="font-size: 2.2rem;"></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SelfReportInfoCards',
    props: {
      selfReportStats: Array,
    },
    mounted() {
      this.cards = this.cards.map((c) => {
        const found = this.selfReportStats.find((s) => c.id === s.value);
        if (found) {
          // eslint-disable-next-line
          c.count = found.count;
        }
        return c;
      });
    },
    data() {
      return {
        cards: [{
          id: 'Approval',
          label: 'Approval Required',
          count: 0,
          icon: 'far fa-thumbs-up',
        }, {
          id: 'HonorSystem',
          label: 'Honor System',
          count: 0,
          icon: 'far fa-meh-rolling-eyes',
        }, {
          id: 'Disabled',
          label: 'Disabled',
          count: 0,
          icon: 'far fa-times-circle',
        }],
      };
    },
  };
</script>

<style scoped>

</style>
