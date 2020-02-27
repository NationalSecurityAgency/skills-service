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
    <div class='card position-relative'>
      <div class='card-body'>
        <i
          :class="iconClass"
          class="watermark-icon"/>
        <div>
          <div style="height: 3rem;">
            <vue-simple-spinner v-if="loading" line-bg-color="#333" line-fg-color="#17a2b8"/>
            <span v-else><h1>{{ value | number }}</h1></span>
          </div>
          <h5>{{ label }}</h5>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import Spinner from 'vue-simple-spinner';

  export default {
    components: {
      'vue-simple-spinner': Spinner,
    },
    props: {
      iconClass: String,
      value: Number,
      label: String,
    },
    computed: {
      loading() {
        return this.value === -1;
      },
    },
  };
</script>

<style scoped>
  .card .card-body i {
    color: #0fcc15d1;
    opacity: 0.18;
    font-size: 6rem;
    position: absolute;
    left: 0;
    top: 50%;
    right: 0;
    margin: auto;
    top: 50%;
    transform: translateY(-50%);
  }
</style>
