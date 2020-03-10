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
  <div class="existingUserInput row no-gutters">
    <b-dropdown v-if="userSuggestOptions && userSuggestOptions.length > 0" variant="split" :text="selectedSuggestOption" class="col-md-auto">
      <b-dropdown-item-button v-for="opt in userSuggestOptions" :key="opt.value" :active="opt.value === selectedSuggestOption" @click="selectedSuggestOption=opt.value">{{opt.value}}</b-dropdown-item-button>
    </b-dropdown>

    <multiselect v-model="userQuery" :placeholder="placeholder" tag-placeholder="Enter to select"
                 :options="suggestions" :multiple="allowMultipleSelections" :taggable="canEnterNewUser" @tag="addTag"
                 :hide-selected="true" track-by="userId" label="label"
                 @search-change="suggestUsers" @open="suggestUsers" :loading="isFetching" :internal-search="false"
                 :clear-on-select="true" :class="{'col-9': (userSuggestOptions && userSuggestOptions.length > 0)}">
    </multiselect>

    <p class="text-danger" v-show="validate && theError">{{ theError }}</p>
  </div>
</template>

<script>
  import axios from 'axios';
  import debounce from 'lodash.debounce';
  import Multiselect from 'vue-multiselect';
  import RequestOrderMixin from './RequestOrderMixin';

  // user type constants
  const DASHBOARD = 'DASHBOARD';
  const CLIENT = 'CLIENT';
  const ROOT = 'ROOT';
  const SUPERVISOR = 'SUPERVISOR';

  export default {
    name: 'ExistingUserInput',
    mixins: [RequestOrderMixin],
    components: { Multiselect },
    props: {
      fieldLabel: {
        default: 'Skills User',
        type: String,
      },
      placeholder: {
        default: 'Enter user id',
        type: String,
      },
      projectId: {
        type: String,
      },
      validate: {
        type: Boolean,
        default: false,
      },
      suggest: {
        type: Boolean,
        default: false,
      },
      allowMultipleSelections: {
        type: Boolean,
        default: false,
      },
      userType: {
        type: String,
        default: CLIENT,
        validator: value => ([DASHBOARD, CLIENT, ROOT, SUPERVISOR].indexOf(value) >= 0),
      },
      excludedSuggestions: {
        type: Array,
        default: () => ([]),
      },
      value: Object,
      canEnterNewUser: {
        type: Boolean,
        default: false,
      },
    },
    mounted() {
      if (this.$store.getters.config && this.$store.getters.config.userSuggestOptions) {
        const opts = this.$store.getters.config.userSuggestOptions.split(',');
        opts.forEach((opt) => {
          this.userSuggestOptions.push({ text: opt, value: opt });
        });
        this.selectedSuggestOption = this.userSuggestOptions[0].value;
      }
    },
    watch: {
      userQuery(newVal) {
        // must be able to handle string or an array as the multiselect lib will place
        // an array if it was selected from the dropdown and a string if it was entered
        if (!newVal || newVal.length === 0) {
          this.$emit('input', null);
        } else if (Array.isArray(newVal)) {
          this.$emit('input', newVal[0]);
        } else {
          this.$emit('input', newVal);
        }
      },
      value(newVal) {
        this.userQuery = newVal;
      },
    },
    data() {
      return {
        isFetching: false,
        suggestions: [],
        selectedUser: null,
        theError: '',
        userQuery: this.value,
        userSuggestOptions: [],
        selectedSuggestOption: null,
      };
    },
    computed: {
      emptySlot() {
        return this.isFetching ? 'loading...' : 'No results found';
      },
      suggestUrl() {
        let suggestUrl;
        if (this.userType === CLIENT) {
          if (this.$store.getters.isPkiAuthenticated) {
            suggestUrl = '/app/users/suggestPkiUsers';
          } else if (this.projectId) {
            suggestUrl = `/app/users/projects/${this.projectId}/suggestClientUsers`;
          } else {
            suggestUrl = '/app/users/suggestClientUsers/';
          }
        } else if (this.userType === SUPERVISOR) {
          suggestUrl = '/root/users/without/role/ROLE_SUPERVISOR';
        } else if (this.userType === ROOT) {
          suggestUrl = '/root/users/without/role/ROLE_SUPER_DUPER_USER';
        } else {
          // userType === DASHBOARD
          suggestUrl = '/app/users/suggestDashboardUsers';
          if (this.$store.getters.isPkiAuthenticated) {
            suggestUrl = '/app/users/suggestPkiUsers';
          }
        }
        return suggestUrl;
      },
      validateUrl() {
        let validateUrl;
        if (this.userType === CLIENT) {
          if (this.projectId) {
            validateUrl = `/app/users/projects/${this.projectId}/validExistingClientUserId/`;
          } else {
            validateUrl = '/app/users/validExistingClientUserId/';
          }
        } else {
          validateUrl = '/app/users/validExistingDashboardUserId/';
        }
        return validateUrl;
      },
    },
    methods: {
      suggestUsers: debounce(function debouncedSuggestUsers(query) {
        this.isFetching = true;
        let q = query;
        if (!q) {
          q = '';
        }
        let url = `${this.suggestUrl}/${encodeURIComponent(q)}`;
        if (q && this.selectedSuggestOption) {
          url += `?userSuggestOption=${this.selectedSuggestOption}`;
        }
        const rid = this.getRequestId();
        axios.get(url)
          .then((response) => {
            this.ensureOrderlyResultHandling(rid, () => {
              this.suggestions = response.data.filter(suggestedUser => !this.excludedSuggestions.includes(suggestedUser.userId));
              this.suggestions = this.suggestions.map((it) => {
                const label = this.getUserIdForDisplay(it);
                const sug = {
                  ...it,
                  label,
                };
                return sug;
              });
            });
          })
          .finally(() => {
            this.isFetching = false;
          });
      }, 200),
      addTag(newTag) {
        const tag = {
          userId: newTag,
          label: newTag,
        };
        this.userQuery = tag;
        this.suggestions.push(tag);
      },

      getUserIdForDisplay(user) {
        if (!user.userIdForDisplay) {
          return user.userId;
        }
        if (user.first && user.last) {
          return `${user.first} ${user.last} (${user.userIdForDisplay})`;
        }
        return user.userIdForDisplay;
      },

      validateUserId(userId) {
        if (userId !== null) {
          if (this.validate) {
            this.theError = '';
            axios.get(`${this.validateUrl}${encodeURIComponent(userId)}`, { errorPage: false })
              .then(response => response.data)
              .then((result) => {
                if (result) {
                  this.onUserSelected(userId);
                } else {
                  this.theError = 'Invalid User Id';
                }
              });
          } else {
            this.onUserSelected(userId);
          }
        }
      },
    },
  };
</script>

<style>
  .existingUserInput .multiselect__content-wrapper {
    background: #f8f9fa;
    /*border-width: 2px;*/
    border-color: #b1b1b1;
  }
</style>
