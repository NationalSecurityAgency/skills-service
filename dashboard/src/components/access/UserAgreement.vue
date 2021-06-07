/*
Copyright 2021 SkillTree

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
  <loading-container :is-loading="loading" class="container-fluid">
    <b-row class="mt-2">
      <b-col>
        <div class="card p-0 m-0">
          <div class="card-body mt-2 mb-0 p-0">
            <h1 class="h4 text-uppercase text-center">User Agreement</h1>
          </div>
        </div>
      </b-col>
    </b-row>

    <div v-if="!loading" class="overflow-auto mt-2 mb-2 mr-5 ml-5">
      <markdown-text data-cy="userAgreement" :text="userAgreement"/>
      <div class="float-right">
        <button class="btn btn-outline-success" type="button" v-on:click="acknowledgeUa"
                data-cy="acknowledgeUserAgreement">
          Acknowledge
          <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
        </button>
      </div>
    </div>
  </loading-container>
  <!-- how do we style this? -->
</template>

<script>
  import VueRouter from 'vue-router';
  import MarkdownText from '../utils/MarkdownText';
  import AccessService from './AccessService';
  import SettingsService from '../settings/SettingsService';
  import LoadingContainer from '../utils/LoadingContainer';

  const { NavigationFailureType, isNavigationFailure } = VueRouter;

  export default {
    name: 'UserAgreement',
    components: { MarkdownText, LoadingContainer },
    data() {
      return {
        userAgreement: '',
        uaVersion: '',
        loading: true,
        isSaving: false,
      };
    },
    mounted() {
      AccessService.getUserAgreement().then((ua) => {
        if (ua) {
          this.userAgreement = ua.userAgreement;
          this.uaVersion = ua.currentVersion;
        }
      }).finally(() => {
        this.loading = false;
      });
    },
    methods: {
      acknowledgeUa() {
        this.isSaving = true;
        const ack = {
          settingGroup: 'user',
          setting: 'viewed_user_agreement',
          value: this.uaVersion,
        };
        SettingsService.saveUserSettings([ack]).then(() => {
          // redirect to original page
          this.$store.commit('showUa', false);
          this.$router.push(this.$route.query.redirect || '/').catch((error) => {
            if (isNavigationFailure(error, NavigationFailureType.redirected)
              || isNavigationFailure(error, NavigationFailureType.duplicated)) {
              // squash, vue-router made changes in version 3 that
              // causes a redirect to trigger an error. router-link squashes these and in previous
              // versions of vue-router they were ignored. Because we trigger redirects in a navigation guard
              // to handle landing/home page display preferences, we receive this benign error
            } else {
              // eslint-disable-next-line
              console.error(error);
            }
          });
        }).finally(() => {
          this.isSaving = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>
