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
    <div class="row skills-no-data-yet text-primary" data-cy="noDataYet">
      <div class="col text-secondary">
        <span v-if="icon"><i :class="icon"></i></span>
        <span v-else class="fa-stack fa-3x" style="vertical-align: top;">
          <i class="fas fa-circle fa-stack-2x"></i>
          <i class="fas fa-battery-empty fa-stack-1x fa-inverse"></i>
        </span>
      </div>
        <div class="w-100"></div>
        <div class="col pt-2">
            <h3>{{ title }}</h3>
        </div>
        <div class="w-100"></div>
        <div class="col">
            {{ subTitle }}
        </div>

    </div>
</template>

<script>
  export default {
    name: 'NoDataYet',
    props: {
      title: String,
      subTitle: String,
      icon: String,
    },
  };
</script>

<style scoped>
    /*.skills-no-data-yet .fa-inverse {*/
    /*    color: yellow !important;*/
    /*}*/
</style>
