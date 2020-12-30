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
  <div class="card h-100">
    <div class="card-body">
      <div class="media">
        <div class="d-inline-block mt-1 mr-3">
          <i class="fa-3x" :class="iconClass"/>
        </div>
        <div class="media-body">
          <div class="h4 mb-2 text-uppercase">{{ title }}</div>
          <div style="font-size: 0.95rem;" class="text-secondary">
            <slot>
              {{ subTitle }}
            </slot>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'MediaInfoCard',
    props: {
      title: String,
      subTitle: String,
      iconClass: String,
    },
  };
</script>

<style scoped>

</style>
