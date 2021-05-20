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
  <div class="p-1 poweredByContainer text-lowercase">
    <div class="poweredByLabel pb-2">powered by</div>
    <img src="img/skilltree_logo.png" class="logo" alt="SkillTree"/>
  </div>
</template>

<script>
  export default {
    name: 'PoweredBySkilltree',
  };
</script>

<style scoped>
.poweredByContainer {
  width: 7.6rem !important;
}
.poweredByContainer .poweredByLabel {
  font-size: 0.8rem !important;
  position: absolute !important;
  bottom: 0.8rem;
}
.poweredByContainer .logo {
  height: 2.2rem !important;
  float: right;
}
</style>
