<template>
  <div class="existingUserInput">
    <b-field :label="fieldLabel">
      <b-autocomplete
        data-vv-name="user"
        data-vv-delay="100"
        v-validate="'required|isValidUser'"
        v-model="userQuery"
        :data="suggestions"
        :placeholder="placeholder"
        :loading="isFetching"
        @input="suggestUsers"
        @select="option => selectedUser = option">
        <template slot="empty">{{ emptySlot }}</template>
      </b-autocomplete>
    </b-field>
    <div v-if="validate && fields && fields.user && fields.user.pending">
      <i class="fa fa-circle-notch fa-spin fa-3x-fa-fw"></i>
      <span>validating user...</span>
    </div>
    <p class="help is-danger" v-show="validate && errors.has('user') && !fields.user.pending">{{ errors.first('user')}}</p>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import debounce from 'lodash.debounce';

  let self;

  // user type constants
  const DASHBOARD = 'DASHBOARD';
  const CLIENT = 'CLIENT';

  export default {
    name: 'ExistingUserInput',
    inject: { $validator: '$validator' },
    props: {
      fieldLabel: {
        default: 'Skills User *',
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
        validator: value => ([DASHBOARD, CLIENT].indexOf(value) >= 0),
      },

    },
    data() {
      return {
        userQuery: null,
        isFetching: false,
        suggestions: [],
        selectedUser: null,
      };
    },
    mounted() {
      self = this;
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
            this.suggestions = response.data;
            this.isFetching = false;
          })
          .finally(() => {
            this.isFetching = false;
        });
      }, 200),
    },
  };

  Validator.extend('isValidUser', {
    getMessage() {
      return 'Invalid User ID';
    },
    validate(value) {
      return new Promise((resolve) => {
        let isValid = false;
        if (!self.validate) {
          isValid = true;
        } else {
          axios.get(`${self.validateUrl}${encodeURIComponent(value)}`, { errorPage: false })
            .then((response) => {
              isValid = response.data;
              resolve({
                valid: isValid,
                data: undefined,
              });
          });
        }
      });
    },
  });
</script>
