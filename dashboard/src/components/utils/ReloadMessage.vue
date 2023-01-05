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
  <div class="alert alert-info">
    Form contents have been restored from backup.
    <b-button style="float: right;" size="sm" @click="discardChanges()">
      <i class="fas fa-trash" aria-hidden="true"/> Discard Changes
    </b-button>
  </div>
</template>

<script>
  export default {
    name: 'ReloadMessage',
    methods: {
      discardChanges() {
        this.$emit('discard-changes', true);
      },
    },
  };
</script>
