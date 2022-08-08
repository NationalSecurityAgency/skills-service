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
  <loading-container :is-loading="loading">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6 mt-3">
          <div class="text-center mt-5">
            <i class="fa fa-users fa-4x text-secondary"></i>
            <h2 class="mt-4 text-info">
              <span>Join Project</span>
            </h2>
            <div v-if="!joined">
              <div class="mb-2">
                <p>Great News! You have been invited to join the <span class="text-primary font-weight-bold">{{ projectName }}</span> Project.</p>
                <p><span class="text-primary font-weight-bold">{{ projectName }}</span> is a gamified micro-learning experience
                built using the SkillTree platform. Explore <span class="text-primary font-weight-bold">{{ projectName }}</span> to see how you can earn points and achievements!
                </p>
              </div>
              <div>
                <b-button @click="join()" variant="outline-primary" :disabled="inviteInvalid"
                          data-cy="joinProject"><i :class="joinIcon" aria-hidden="true" /> Join Now</b-button>
              </div>
            </div>
            <div v-if="joined">
              You have successfully joined Project {{ projectName }}! You will be forwarded to
              <router-link :to="{ name:'MyProjectSkills', params: { projectId: pid, name: projectName } }"
                            tag="a"
                           class="project-link" :data-cy="`project-link-${pid}`" tabindex="0">{{ projectName }}</router-link> in {{ timer }} seconds.

            </div>
            <div v-if="inviteInvalid" class="mt-3 text-danger" role="alert">{{ invalidMsg }}</div>
          </div>
        </div>
      </div>
    </div>
  </loading-container>
</template>

<script>
  import AccessService from '@/components/access/AccessService';
  import LoadingContainer from '@/components/utils/LoadingContainer';

  export default {
    name: 'JoinProject',
    props: {
      inviteToken: {
        type: String,
        required: true,
      },
      pid: {
        type: String,
        required: true,
      },
      projectName: {
        type: String,
        required: true,
      },
    },
    components: { LoadingContainer },
    data() {
      return {
        joinInProgress: false,
        joinFailed: false,
        joinSuccessful: false,
        remoteError: null,
        inviteInvalid: false,
        loading: false,
        invalidMsg: '',
        joining: false,
        joined: false,
        timer: -1,
      };
    },
    mounted() {
      this.loading = true;
      AccessService.isInviteValid(this.pid, this.inviteToken).then((resp) => {
        this.inviteInvalid = !resp.valid;
        this.invalidMsg = resp.message;
      }).finally(() => {
        this.loading = false;
      });
    },
    computed: {
      joinIcon() {
        return this.joining ? 'fas fa-spinner' : 'fas fa-unlock';
      },
    },
    watch: {
      timer(value) {
        if (value > 0) {
          setTimeout(() => {
            this.timer -= 1;
          }, 1000);
        } else {
          this.$router.replace({ name: 'MyProjectSkills', params: { projectId: this.pid, name: this.projectName } });
        }
      },
    },
    methods: {
      join() {
        if (!this.inviteInvalid) {
          this.joining = true;
          AccessService.joinProject(this.pid, this.inviteToken).then(() => {
            this.joined = true;
            this.$announcer.polite(`Successfully joined project ${this.projectName}, loading project profile`);
            this.timer = 10;
          }).finally(() => {
            this.joining = false;
          });
        }
      },

    },
  };
</script>

<style scoped>
.project-link {
  text-decoration: underline;
}

.project-link :hover {
  cursor: pointer;
}
</style>
