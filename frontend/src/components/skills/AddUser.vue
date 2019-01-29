<template>
  <div class="modal-card" style="width: 1110px; height: 550px;">
    <header class="modal-card-head">
      <p class="modal-card-title">Add Skill [{{skillNameInternal}}] To User</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <section class="modal-card-body">
      <div class="columns" style="margin-right: 10px;">
        <div class="column">
          <existing-user-input :project-id="projectId" ref="userIdField"></existing-user-input>
        </div>
        <div class="column is-4">
          <b-field label="Date *">
            <b-datepicker
              name="date"
              v-validate = "'required'"
              class="is-small"
              placeholder="Select date of skill"
              v-model="dateAdded">
            </b-datepicker>
          </b-field>
          <p class="help is-danger" v-show="errors.has('date')">{{ errors.first('date')}}</p>
        </div>
        <div class="column is-1">
          <p class="control" style="margin-top:2em">
            <button class="button is-primary is-outlined" v-on:click="addSkill" :disabled="errors.any()">
              <span>Add</span>
              <span class="icon is-small">
                <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
              </span>
            </button>
          </p>
        </div>
      </div>

      <div>
        <ul id="usesAddedForSkill" class="skills-pad-left-1-rem">
          <li style="" v-for="(user) in reversedUsersAdded" v-bind:key="user.key">
            <div class="columns">
              <div class="column is-narrow" :class="[user.success ? 'has-text-success' : 'has-text-danger']" style="width:1.5em;">
                <i :class="[user.success ? 'fa fa-check' : 'fa fa-info-circle']"></i>
              </div>
              <div class="column">
                <span :class="[user.success ? 'has-text-success' : 'has-text-danger']" style="font-weight: bolder">
                  <span v-if="user.success">
                    Added points for
                  </span>
                  <span v-else>
                    Wasn't able to add points for
                  </span>
                  <span>'{{user.userId}}'</span>
                </span><span v-if="!user.success"> - {{user.msg}}</span>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <button class="button is-link is-outlined" v-on:click="$parent.close()">
        <span>Close</span>
          <span class="icon is-small">
            <i class="fas fa-stop-circle"></i>
          </span>
      </button>
    </footer>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import ExistingUserInput from '../utils/ExistingUserInput';

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
    name: 'AddUser',
    props: ['skillId', 'user', 'projectId', 'skillName'],
    components: { ExistingUserInput },
    data() {
      return {
        overallErrMsg: '',
        overallInfoMsg: '',
        isFetching: false,
        suggestions: [],
        dateAdded: new Date(),
        skillNameInternal: this.skillName,
        usersAdded: [],
        isSaving: false,
      };
    },
    computed: {
      reversedUsersAdded: function reversedUsersAdded() {
        return this.usersAdded.map(e => e).reverse();
      },
    },
    methods: {
      addSkill() {
        this.isSaving = true;
        axios.put(`/admin/projects/${this.projectId}/userSkills/${this.skillId}`, {
          userId: this.$refs.userIdField.$data.userQuery,
          timestamp: this.dateAdded.getTime(),
        }).then((skillAddedResult) => {
          this.isSaving = false;
          const data = skillAddedResult.data;
          const historyObj = { success: data.wasPerformed, msg: data.explanation, userId: this.$refs.userIdField.$data.userQuery, key: this.$refs.userIdField.$data.userQuery + new Date().getTime() + data.wasPerformed };
          this.usersAdded.push(historyObj);
        })
          .catch((e) => {
            this.isSaving = false;
            throw e;
        });
      },
    },
  };

</script>
