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
    <sub-page-header title="Email Settings"/>
    <div class="card mb-3">
      <div class="card-header">Email Connection Settings</div>
      <div class="card-body">
        <email-server-settings data-cy="emailConnectionSettings"/>
      </div>
    </div>
    <div class="card">
      <div class="card-header">Email Template Settings</div>
      <div class="card-body">
        <email-template-settings data-cy="emailTemplateSettings"/>
      </div>
    </div>
  </div>
</template>

<script>
  import EmailServerSettings from './EmailServerSettings';
  import EmailTemplateSettings from './EmailTemplateSettings';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'EmailSettings',
    components: { EmailTemplateSettings, SubPageHeader, EmailServerSettings },
  };
</script>

<style scoped>

</style>
