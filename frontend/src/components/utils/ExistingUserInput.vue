<template>
  <div class="existingUserInput">
    <b-field :label="fieldLabel">
      <b-autocomplete
        data-vv-name="user"
        v-validate="'required|suggestUser'"
        data-vv-delay="100"
        v-model="userQuery"
        :data="suggestions"
        :placeholder="placeholder"
        :loading="isFetching"
        @input="suggestUsers"
        @select="option => selectedUser = option">
        <template slot="empty">{{ emptySlot }}</template>
      </b-autocomplete>
    </b-field>
    <div style="min-height: 1rem">
      <div v-if="validating && fields && fields.user && fields.user.pending" class="help is-black">
        <i class="fa fa-circle-notch fa-spin fa-3x-fa-fw"></i>
        <span>Validating user...</span>
      </div>
      <div v-else-if="validating && errors.has('user') && !fields.user.pendin">
        <span class="help is-danger">{{ errors.first('user')}}</span>
      </div>
    </div>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import debounce from 'lodash.debounce';
  import ToastHelper from './ToastHelper';

  let self = null;

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
    },
    data() {
      return {
        userQuery: null,
        isFetching: false,
        suggestions: [],
        selectedUser: null,
        validating: true,
      };
    },
    computed: {
      emptySlot() {
        return this.isFetching ? 'loading...' : 'No results found';
      },
      suggestUrl() {
        let suggestUrl = `/admin/projects/${this.projectId}/suggestUsers/${this.userQuery}`;
        if (!this.projectId) {
          suggestUrl = `/admin/suggestUsers/${this.userQuery}`;
        }
        return suggestUrl;
      },
      validateUrl() {
        let validateUrl = `/admin/projects/${this.projectId}/validExistingUserId/`;
        if (!this.projectId) {
          validateUrl = 'admin/validExistingUserId/';
        }
        return validateUrl;
      },
    },
    mounted() {
      self = this;
    },
    methods: {
      suggestUsers: debounce(function debouncedSuggestUsers() {
        if (!this.userQuery) {
          this.suggestions = [];
          return;
        }

        this.isFetching = true;
        axios.get(this.suggestUrl)
          .then((response) => {
            this.suggestions = response.data;
            this.isFetching = false;
          })
          .catch((e) => {
            this.isFetching = false;
            this.suggestions = [];
            throw e;
        });
      }, 200),
    },
  };

  const ctx = this;

  Validator.extend('suggestUser', {
    getMessage() {
      return 'No matching users found';
    },
    validate(value) {
      // consider checking this.fields[user].dirty before
      // running the ajax validation so that we don't validate a second time
      // on form submission if the field is already valid
      // we would need to save off the previous validation result somewhere
      // so that if this.fields[user].dirty is false, we just return the last validation result
      return new Promise((resolve) => {
        let isValid = false;

        axios.get(self.validateUrl + value)
          .then((response) => {
            isValid = response.data;
            resolve({
              valid: isValid,
              data: undefined,
            });
          })
          .catch((e) => {
            ctx.$toast.open(ToastHelper.defaultConf(`Critical Error encountered. Unable to validate supplied user ID. Failure=[${e.toString()}]`, true));
            throw e;
        });
      });
    },
  });
</script>
