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
  <span>
    <b-badge v-for="item in options" :key="item.value"
             class="mr-2"
             :variant="getVariant(item)"
             :class="{'click-me-cursor' : item.value !== value}"
             @click="updateValue(item.value)">{{ item.label }}</b-badge>
  </span>
</template>

<script>
  export default {
    name: 'BadgeBasedSelector',
    props: ['options', 'value'],
    methods: {
      updateValue(newValue) {
        this.$emit('input', newValue);
        this.$emit('value-changed', newValue);
      },
      getVariant(item) {
        if (this.value === item.value) {
          return 'primary';
        }
        return 'secondary';
      },
    },
  };
</script>

<style scoped>
.click-me-cursor {
  cursor: pointer;
}
</style>
