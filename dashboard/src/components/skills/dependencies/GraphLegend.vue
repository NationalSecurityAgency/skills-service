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
  <simple-card class="legend" style="max-width: 18rem; min-width: 17rem;">
    <div class='legend-title'>Color Legend</div>
    <div class='legend-scale'>
      <ul class='legend-labels'>
        <li v-for="item in items" v-bind:key="item.label">
          <span v-bind:style=' {background: item.color }'></span>{{ item.label }}
        </li>
      </ul>
    </div>
  </simple-card>
</template>

<script>
  import SimpleCard from '../../utils/cards/SimpleCard';

  export default {
    name: 'GraphLegend',
    components: { SimpleCard },
    props: ['items'],
  };
</script>

<style scoped>
  .legend .legend-title {
    text-align: left;
    margin-bottom: 5px;
    font-weight: bold;
    font-size: 90%;
  }

  .legend .legend-scale ul {
    margin: 0;
    margin-bottom: 5px;
    padding: 0;
    float: left;
    list-style: none;
  }

  .legend .legend-scale ul li {
    font-size: 80%;
    list-style: none;
    margin-left: 0;
    line-height: 18px;
    margin-bottom: 2px;
  }

  .legend ul.legend-labels li span {
    display: block;
    float: left;
    height: 16px;
    width: 30px;
    margin-right: 5px;
    margin-left: 0;
    border: 1px solid #999;
  }

  .legend a {
    color: #777;
  }
</style>
