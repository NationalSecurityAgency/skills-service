<template>
  <div class="existingUserInput">
    <b-field :label="fieldLabel">
      <b-autocomplete
        v-model="userQuery"
        :data="suggestions"
        :placeholder="placeholder"
        :loading="isFetching"
        @input="suggestUsers"
        icon="fas fa-search"
        @select="validateUserId">
        <template slot="empty">{{ emptySlot }}</template>
      </b-autocomplete>
    </b-field>
    <p class="help is-danger" v-show="validate && theError">{{ theError }}</p>
  </div>
</template>

<script>
  import axios from 'axios';
  import debounce from 'lodash.debounce';

  // user type constants
  const DASHBOARD = 'DASHBOARD';
  const CLIENT = 'CLIENT';
  const ROOT = 'ROOT';

  export default {
    name: 'ExistingUserInput',
    props: {
      fieldLabel: {
        default: 'Skills User',
        type: String,
      },
      placeholder: {
        default: 'enter user id',
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
      userType: {
        type: String,
        default: CLIENT,
        validator: value => ([DASHBOARD, CLIENT, ROOT].indexOf(value) >= 0),
      },
      excludedSuggestions: {
        type: Array,
        default: () => ([]),
      },
      selectedUserId: {
        type: String,
      },
    },
    watch: {
      selectedUserId(newVal) {
        this.userQuery = newVal;
      },
    },
    data() {
      return {
        isFetching: false,
        suggestions: [],
        selectedUser: null,
        theError: '',
        userQuery: this.selectedUser,
      };
    },
    computed: {
      emptySlot() {
        return this.isFetching ? 'loading...' : 'No results found';
      },
      suggestUrl() {
        let suggestUrl;
        if (this.userType === CLIENT) {
          if (this.projectId) {
            suggestUrl = `/app/users/projects/${this.projectId}/suggestClientUsers/${this.userQuery}`;
          } else {
            suggestUrl = `/app/users/suggestClientUsers/${this.userQuery}`;
          }
        } else if (this.userType === ROOT) {
          suggestUrl = `/root/users/${this.userQuery}`;
        } else {
          suggestUrl = `/app/users/suggestDashboardUsers/${this.userQuery}`;
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
      suggestUsers: debounce(function debouncedSuggestUsers() {
        if (!this.suggest || !this.userQuery) {
          this.suggestions = [];
          return;
        }
        this.isFetching = true;
        axios.get(this.suggestUrl)
          .then((response) => {
            this.suggestions = response.data.filter(suggestedUserId => !this.excludedSuggestions.includes(suggestedUserId));
          })
          .finally(() => {
            this.isFetching = false;
          });
      }, 200),
      onUserSelected(userId) {
        this.$emit('userSelected', userId);
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
