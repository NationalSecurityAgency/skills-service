<template>
  <div>
    <sub-page-header title="Add Skill Events"/>
    <simple-card style="min-height: 20rem;">
      <div id="add-user-div" class="row mt-2 mb-4">
        <div class="col-12 col-md-6 pb-2 pb-md-0">
          <existing-user-input :project-id="projectId" v-model="currentSelectedUser" :can-enter-new-user="!pkiAuthenticated"/>
        </div>
        <div class="col-auto">
          <datepicker input-class="border-0" wrapper-class="form-control" v-model="dateAdded" name="Event Date" v-validate="'required'"/>
        </div>
        <div class="col-auto">
          <b-button variant="outline-primary" @click="addSkill" :disabled="errors.any() || (!currentSelectedUser || !currentSelectedUser.userId || currentSelectedUser.userId.length === 0) || projectTotalPoints < minimumPoints">
            Add <i v-if="projectTotalPoints >= minimumPoints" :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
            <i v-else class="icon-warning fa fa-exclamation-circle text-warning"
               v-b-tooltip.hover="'Unable to add skill for user. Insufficient available points in project.'"></i>
          </b-button>
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
    },
    methods: {
      loadProject() {
        ProjectService.getProject(this.projectId).then((res) => {
          this.projectTotalPoints = res.totalPoints;
        });
      },
      addSkill() {
        this.isSaving = true;
        SkillsService.saveSkillEvent(this.$route.params.projectId, this.$route.params.skillId, this.currentSelectedUser.userId, this.dateAdded.getTime(), this.pkiAuthenticated)
          .then((data) => {
            this.isSaving = false;
            const historyObj = {
              success: data.skillApplied,
              msg: data.explanation,
              userId: this.currentSelectedUser.userId,
              key: this.currentSelectedUser.uesrId + new Date().getTime() + data.skillApplied,
            };
            this.usersAdded.push(historyObj);
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
    },
  };

</script>
