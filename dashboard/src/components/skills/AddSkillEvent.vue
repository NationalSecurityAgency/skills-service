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
    <sub-page-header title="Add Skill Events"/>
    <simple-card style="min-height: 20rem;">
      <ValidationObserver v-slot="{invalid}" slim>
        <div id="add-user-div" class="row mt-2 mb-4">
          <div class="col-12 col-md-6 pb-2 pb-md-0">
            <ValidationProvider name="User Id" v-slot="{errors}" rules="userNoSpaceInUserIdInNonPkiMode">
              <existing-user-input :project-id="projectId" v-model="currentSelectedUser" :can-enter-new-user="!pkiAuthenticated"
                                   name="User Id" data-cy="userIdInput"/>
              <small class="form-text text-danger" v-show="errors[0]">{{ errors[0]}}</small>
            </ValidationProvider>
          </div>
          <div class="col-auto">
            <ValidationProvider name="Event Date" rules="required">
              <datepicker input-class="border-0" wrapper-class="form-control" v-model="dateAdded" name="Event Date"
                          :use-utc="true" :disabled-dates="datePickerState.disabledDates" aria-required="true" aria-label="event date"/>
            </ValidationProvider>
          </div>
          <div class="col-auto">
            <div v-b-tooltip.hover :title="minPointsTooltip">
              <b-button variant="outline-primary" @click="addSkill" :disabled="invalid || disable" v-skills="'ManuallyAddSkillEvent'" data-cy="addSkillEventButton">
                Add <i v-if="projectTotalPoints >= minimumPoints" :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
                <i v-else class="icon-warning fa fa-exclamation-circle text-warning"></i>
              </b-button>
            </div>
          </div>
        </div>
      </ValidationObserver>

      <div class="row mt-2" v-for="(user) in reversedUsersAdded" v-bind:key="user.key">
        <div class="col">
          <span :class="[user.success ? 'text-success' : 'text-danger']" style="font-weight: bolder">
            <i :class="[user.success ? 'fa fa-check' : 'fa fa-info-circle']" aria-hidden="true"/>
            <span v-if="user.success">
              Added points for
            </span>
            <span v-else>
              Wasn't able to add points for
            </span>
            <span>[{{user.userIdForDisplay ? user.userIdForDisplay : user.userId }}]</span>
          </span><span v-if="!user.success"> - {{user.msg}}</span>
        </div>
      </div>

    </simple-card>
  </div>
</template>

<script>
  import Datepicker from 'vuejs-datepicker';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import SkillsService from './SkillsService';
  import ProjectService from '../projects/ProjectService';

  const disabledDates = (date) => date > new Date();

  const datePickerState = {
    disabledDates: {
      customPredictor: disabledDates,
    },
  };

  export default {
    name: 'AddSkillEvent',
    components: {
      ExistingUserInput,
      SimpleCard,
      SubPageHeader,
      Datepicker,
    },
    props: {
      projectId: {
        type: String,
      },
    },
    data() {
      return {
        overallErrMsg: '',
        overallInfoMsg: '',
        isFetching: false,
        suggestions: [],
        dateAdded: new Date(),
        usersAdded: [],
        isSaving: false,
        currentSelectedUser: null,
        projectTotalPoints: 0,
        pkiAuthenticated: false,
        datePickerState,
      };
    },
    mounted() {
      this.loadProject();
      this.pkiAuthenticated = this.$store.getters.isPkiAuthenticated;
    },
    computed: {
      reversedUsersAdded: function reversedUsersAdded() {
        return this.usersAdded.map((e) => e)
          .reverse();
      },
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      minimumSubjectPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
      },
      disable() {
        return (!this.currentSelectedUser || !this.currentSelectedUser.userId || this.currentSelectedUser.userId.length === 0) || this.projectTotalPoints < this.minimumPoints;
      },
      minPointsTooltip() {
        let text = '';
        if (this.projectTotalPoints < this.minimumPoints) {
          text = 'Unable to add skill for user. Insufficient available points in project.';
        }
        return text;
      },
    },
    methods: {
      loadProject() {
        ProjectService.getProject(this.projectId).then((res) => {
          this.projectTotalPoints = res.totalPoints;
        });
      },
      addSkill() {
        this.isSaving = true;
        SkillsService.saveSkillEvent(this.$route.params.projectId, this.$route.params.skillId, this.currentSelectedUser, this.dateAdded.getTime(), this.pkiAuthenticated)
          .then((data) => {
            this.isSaving = false;
            const historyObj = {
              success: data.skillApplied,
              msg: data.explanation,
              userId: this.currentSelectedUser.userId,
              userIdForDisplay: this.currentSelectedUser.userIdForDisplay,
              key: this.currentSelectedUser.userId + new Date().getTime() + data.skillApplied,
            };
            this.usersAdded.push(historyObj);
            this.currentSelectedUser = null;
          })
          .catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'UserNotFound') {
              this.isSaving = false;
              const historyObj = {
                success: false,
                msg: e.response.data.explanation,
                userId: this.currentSelectedUser.userId,
                userIdForDisplay: this.currentSelectedUser.userIdForDisplay,
                key: this.currentSelectedUser.userId + new Date().getTime() + false,
              };
              this.usersAdded.push(historyObj);
              this.currentSelectedUser = null;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
              this.$router.push({ name: 'ErrorPage', query: { errorMessage } });
            }
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
    },
  };

</script>
