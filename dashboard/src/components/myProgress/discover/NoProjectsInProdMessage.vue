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
  <b-card class="p-4 text-center">

    <no-content2 title="No Discoverable Projects"/>
    <p class="text-primary">
      Projects can be created and managed from the "Project Admin" view.
    </p>

    <b-row class="justify-content-md-center mt-4">
      <b-col cols="12" md="auto" class="text-left">
        <div class="h5 text-center">
          Instructions:
        </div>
        <div>
          <b-badge class="mr-1">1.</b-badge>
          click on the <img src="/static/img/screenshot_settingsButton.png"
                            alt="Settings button screenshot"/> button on the top right and select <img
          src="/static/img/screenshot_projAdminNav.png" alt="Admin navigation button screenshot"/>
        </div>
        <div class="mt-2">
          <b-badge class="mr-1">2.</b-badge>
          Create or edit a project and then navigate to the <img
          src="/static/img/screenshot_set
          tingsTab.png" alt="Settings tab screenshot"/> tab
        </div>
        <div class="mt-2">
          <b-badge class="mr-1">3.</b-badge>
          Enable the <img class="border rounded"
                          src="/static/img/screenshot_discoverable_setting1.png" alt="Discoverable Setting"/> setting
        </div>
      </b-col>
    </b-row>
  </b-card>
</template>

<script>
  import NoContent2 from '../../utils/NoContent2';

  export default {
    name: 'NoProjectsInProdMessage',
    components: { NoContent2 },
  };
</script>

<style scoped>

</style>
