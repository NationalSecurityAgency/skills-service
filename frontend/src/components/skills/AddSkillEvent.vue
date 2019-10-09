<template>
  <div>
    <sub-page-header title="Add Skill Events"/>
    <simple-card style="min-height: 20rem;">
      <div id="add-user-div" class="row mt-2 mb-4">
        <div class="col-12 col-md-6 pb-2 pb-md-0">
          <existing-user-input :project-id="projectId" v-model="currentSelectedUser" :can-enter-new-user="!pkiAuthenticated"/>
        </div>
        <div class="col-auto">
          <datepicker input-class="border-0" wrapper-class="form-control" v-model="dateAdded" name="Event Date" v-validate="'required'"
                      :use-utc="true" :disabled-dates="datePickerState.disabledDates"/>
        </div>
        <div class="col-auto">
          <div v-b-tooltip.hover="generateMinPointsTooltip">
            <b-button variant="outline-primary" @click="addSkill" :disabled="errors.any() || disable" v-skills="'ManuallyAddSkillEvent'">
              Add <i v-if="projectTotalPoints >= minimumPoints" :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
              <i v-else class="icon-warning fa fa-exclamation-circle text-warning"></i>
            </b-button>
          </div>
        </div>
      </div>

      <div class="row mt-2" v-for="(user) in reversedUsersAdded" v-bind:key="user.key">
        <div class="col">
          <span :class="[user.success ? 'text-success' : 'text-danger']" style="font-weight: bolder">
            <i :class="[user.success ? 'fa fa-check' : 'fa fa-info-circle']"></i>
            <span v-if="user.success">
              Added points for
            </span>
            <span v-else>
              Wasn't able to add points for
            </span>
            <span>[{{user.userId}}]</span>
          </span><span v-if="!user.success"> - {{user.msg}}</span>
        </div>
      </div>

    </simple-card>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import Datepicker from 'vuejs-datepicker';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import SkillsService from './SkillsService';
  import ProjectService from '../projects/ProjectService';

  const dictionary = {
    en: {
      attributes: {
        user: 'User',
        skillId: 'Skill ID',
        date: 'Date',
      },
    },
  };
  Validator.localize(dictionary);

  const disabledDates = date => date.getTime() > Date.now();

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
        return this.usersAdded.map(e => e)
          .reverse();
      },
      minimumPoints() {
        return this.$store.state.minimumProjectPoints;
      },
      minimumSubjectPoints() {
        return this.$store.state.minimumSubjectPoints;
      },
      disable() {
        return (!this.currentSelectedUser || !this.currentSelectedUser.userId || this.currentSelectedUser.userId.length === 0) || this.projectTotalPoints < this.minimumPoints;
      },
    },
    methods: {
      generateMinPointsTooltip() {
        let text = '';
        if (this.projectTotalPoints < this.minimumPoints) {
          text = 'Unable to add skill for user. Insufficient available points in project.';
        }
        return text;
      },
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
              key: this.currentSelectedUser.userId + new Date().getTime() + data.skillApplied,
            };
            this.usersAdded.push(historyObj);
            this.currentSelectedUser = null;
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
    },
  };

</script>
