<template>
  <div class="existingUserInput">
    <multiselect v-model="userQuery" :placeholder="placeholder"
                 :options="suggestions" :multiple="true" :taggable="false"
                 :hide-selected="true"
                 @search-change="suggestUsers" :loading="isFetching" :internal-search="false"
                 v-on:select="onSelected" v-on:remove="onRemoved"
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
            suggestUrl = `/app/users/projects/${this.projectId}/suggestClientUsers`;
          } else {
            suggestUrl = '/app/users/suggestClientUsers/';
          }
        } else if (this.userType === ROOT) {
          suggestUrl = '/root/users';
        } else {
          suggestUrl = '/app/users/suggestDashboardUsers';
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
        // if (!this.suggest || !this.userQuery) {
        //   this.suggestions = [];
        //   return;
        // }
        if (!query) {
          this.suggestions = [];
          return;
        }
        const url = `${this.suggestUrl}/${query}`;
        this.isFetching = true;
        axios.get(url)
          .then((response) => {
            this.suggestions = response.data.filter(suggestedUserId => !this.excludedSuggestions.includes(suggestedUserId));
          })
          .finally(() => {
            this.isFetching = false;
          });
      }, 200),

      onSelected(selectedItem) {
        this.$emit('userSelected', selectedItem);
      },
      onRemoved(item) {
        this.$emit('userRemoved', item);
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
  .existingUserInput .multiselect__tag {
    background-color: lightblue;
    color: black;
  }
</style>
