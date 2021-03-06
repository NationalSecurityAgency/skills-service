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
  <span class="mx-3 mr-4 message-center" v-b-tooltip.hover="'You have 3 critical alerts to review'">
    <span class="text-primary msg-container mr-1" data-count="3" style="font-size: 0.8rem">
      <i class="fas fa-bell" style="font-size: 1.55rem"></i>
    </span>
  </span>
</template>

<script>
  export default {
    name: 'MessageCenter',
  };
</script>

<style scoped>
.message-center:hover {
  cursor: pointer;
}
.msg-container {
  display: inline-block;
  height: 2em;
  line-height: 2em;
  position: relative;
  vertical-align: middle;
  width: 2.5em;
}

.msg-container[data-count]:after{
  position:absolute;
  right:-10%;
  top:-1%;
  content: attr(data-count);
  font-size:60%;
  padding:.6em;
  border-radius:999px;
  line-height:.75em;
  color: white;
  background:rgba(255,0,0,.85);
  text-align:center;
  min-width:2em;
  font-weight:bold;
}

.fa-stack[data-count]:after{
  position:absolute;
  right:-10%;
  top:-1%;
  content: attr(data-count);
  font-size:60%;
  padding:.6em;
  border-radius:999px;
  line-height:.75em;
  color: white;
  background:rgba(255,0,0,.85);
  text-align:center;
  min-width:2em;
  font-weight:bold;
}
</style>
