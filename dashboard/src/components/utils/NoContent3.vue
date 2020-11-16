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
  <div class="card">
    <div class="card-body row text-secondary text-center">
      <div class="col">
                    <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-heart-broken fa-stack-1x fa-inverse"></i>
                    </span>
      </div>
      <div class="w-100"></div>
      <div class="col pt-2">
        <h2 class="h3">{{ title }}</h2>
      </div>
      <div class="w-100"></div>
      <div class="col">
        {{ subTitle }}
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'NoContent3',
    props: {
      title: String,
      subTitle: String,
    },
  };
</script>

<style scoped>

</style>
