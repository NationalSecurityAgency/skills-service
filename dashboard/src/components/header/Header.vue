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
  <div class="bg-white">
    <div class="container-fluid py-3">
      <div class="row">
        <div class="col-sm text-center text-sm-left">
          <router-link class="h2 text-primary ml-2" to="/">
            <img src="/img/skilltree_logo.svg" style="height: 70px; position: absolute; top:-15px; left:15px;"/>
          </router-link>
        </div>

        <div class="col-sm text-center text-sm-right pt-2 mt-3 mt-sm-0">
          <inception-button class="mr-2"></inception-button>
          <settings-button class="mr-2"/>
          <help-button class=""/>
        </div>
      </div>
    </div>
    <breadcrumb></breadcrumb>
  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';
  import InceptionButton from '../inception/InceptionButton';
  import HelpButton from './HelpButton';

  export default {
    name: 'Header',
    components: {
      HelpButton,
      InceptionButton,
      Breadcrumb,
      SettingsButton,
    },
  };
</script>

<style scoped>

</style>
