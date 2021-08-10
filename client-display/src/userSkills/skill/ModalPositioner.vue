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
    <span></span>
</template>

<script>
  import DevModeUtil from '@/dev/DevModeUtil';

  export default {
    name: 'ModalPositioner',
    props: ['yOffset'],
    mounted() {
      if (!DevModeUtil.isDevelopmentMode()) {
        this.$nextTick(() => {
          const modalElem = document.querySelector('.modal-dialog');
          const elemHeight = 270;
          const marginTop = `${this.yOffset - elemHeight}px`;
          modalElem.style['margin-top'] = marginTop;
        });
      }
    },
  };
</script>

<style scoped>

</style>
