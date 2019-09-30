<template>
  <div class="existingUserInput">
    <multiselect v-model="userQuery" :placeholder="placeholder" tag-placeholder="Enter to select"
                 :options="suggestions" :multiple="allowMultipleSelections" :taggable="canEnterNewUser" @tag="addTag"
                 :hide-selected="true" track-by="userId" label="label"
                 @search-change="suggestUsers" @open="suggestUsers" :loading="isFetching" :internal-search="false"
                 :clear-on-select="true">
    </multiselect>

    <p class="text-danger" v-show="validate && theError">{{ theError }}</p>
  </div>
</template>

<script>
  import axios from 'axios';
  import debounce from 'lodash.debounce';
  import Multiselect from 'vue-multiselect';

  // user type constants
  const DASHBOARD = 'DASHBOARD';
  const CLIENT = 'CLIENT';
  const ROOT = 'ROOT';
  const SUPERVISOR = 'SUPERVISOR';

  export default {
    name: 'ExistingUserInput',
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
        const url = `${this.suggestUrl}/${q}`;
        axios.get(url)
          .then((response) => {
            this.suggestions = response.data.filter(suggestedUser => !this.excludedSuggestions.includes(suggestedUser.userId));
            this.suggestions = this.suggestions.map((it) => {
              const label = it.first && it.last ? `${it.first} ${it.last} (${it.userId})` : it.userId;
              const sug = {
                ...it,
                label,
              };
              return sug;
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
