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
  <div class="card" id="trusted-client-props-panel" data-cy="trusted-client-props-panel">
    <div class="card-header">
      Trusted Client Properties
    </div>
    <div class="card-body">
      <div class="row">
        <div class="col-12 col-md-3 text-secondary">
          <span>Client ID:</span>
        </div>
        <div class="col">
          <span>{{ project.projectId }}</span>
        </div>
      </div>
      <div class="row mt-1">
        <div class="col-12 col-md-3 text-secondary">
          <span>Client Secret:</span>
        </div>
        <div class="col">
          <b-spinner v-if="!clientSecret" small label="Loading..." variant="info"/>
          <span>{{ clientSecret }}</span>
        </div>
      </div>
      <b-button @click="confirmResetClientSecret" variant="outline-info" class="mt-3">
        <i class="fas fa-sync-alt"/> Reset Client Secret
      </b-button>
    </div>
  </div>
</template>

<script>
  import AccessService from './AccessService';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'TrustedClientProps',
    mixins: [MsgBoxMixin],
    props: ['project'],
    data() {
      return {
        loadingSecret: true,
        clientSecret: '',
      };
    },
    mounted() {
      AccessService.getClientSecret(this.project.projectId)
        .then((clientSecret) => {
          this.clientSecret = clientSecret;
        });
    },
    methods: {
      confirmResetClientSecret() {
        this.msgConfirm('Are you sure you want reset the client secret? Your current client secret will no longer work after reset and you will need to update any application configuration using the old secret.', 'Reset Secret?', 'Reset Please!')
          .then((res) => {
            if (res) {
              this.resetClientSecret();
            }
          });
      },
      resetClientSecret() {
        AccessService.resetClientSecret(this.project.projectId)
          .then((clientSecret) => {
            this.project.clientSecret = clientSecret;
          });
      },
    },
  };
</script>

<style scoped>
</style>
