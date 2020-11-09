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
    <b-badge v-if="level <= 2" variant="warning">Level {{ level }}</b-badge>
    <b-badge v-if="level > 2 && level <=4" variant="info">Level {{ level }}</b-badge>
    <b-badge v-if="level >= 5" variant="success">Level {{ level }}</b-badge>
  </div>
</template>

<script>
  export default {
    name: 'LevelBadge',
    props: ['level'],
  };
</script>

<style scoped>

</style>
