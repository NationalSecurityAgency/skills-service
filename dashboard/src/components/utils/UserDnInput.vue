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
// THIS IS UNUSED
<template>
  <div class="userDnInput">
    <b-field :label="fieldLabel">
      <b-autocomplete
        data-vv-name="user"
        v-validate="'required|suggestDn'"
        data-vv-delay="100"
        v-model="userDn"
        :data="suggestions"
        placeholder="enter username or dn"
        :loading="isFetching"
        @input="suggestDns"
        @select="option => selected = option">
        <template slot="empty">{{ emptySlot }}</template>
      </b-autocomplete>
    </b-field>
    <div v-if="validating && fields && fields.user && fields.user.pending">
      <i class="fa fa-circle-notch fa-spin fa-3x-fa-fw"></i>
      <span>validating user...</span>
    </div>
    <p class="help is-danger" v-show="validating && errors.has('user') && !fields.user.pending">{{ errors.first('user')}}</p>
  </div>
</template>

<script>
  import axios from 'axios';
  import { Validator } from 'vee-validate';
  import debounce from 'lodash.debounce';
  import RequestOrderMixin from './RequestOrderMixin';

  export default {
    name: 'UserDnInput',
    inject: { $validator: '$validator' },
    mixins: [RequestOrderMixin],
    props: {
      user: String,
      fieldLabel: {
        default: 'User *',
        type: String,
      },
    },
    data() {
      return {
        userDn: this.user,
        isFetching: false,
        suggestions: [],
        selected: null,
        validating: true,
      };
    },
    computed: {
      emptySlot: function getEmptySlot() {
        return this.isFetching ? 'loading...' : 'No results found';
      },
    },
    methods: {
      suggestDns: debounce(function debouncedSuggestDns() {
        if (!this.userDn.length) {
          this.suggestions = [];
          return;
        }

        this.isFetching = true;
        const rid = this.getRequestId();
        axios.get(`/app/users/suggestDns/${encodeURIComponent(this.userDn)}`)
          .then((response) => {
            this.ensureOrderlyResultHandling(rid, () => {
              this.suggestions = response.data;
            });
          })
          .finally(() => {
            this.isFetching = false;
          });
      }, 200),
    },
  };

  Validator.extend('suggestDn', {
    getMessage() {
      return 'Invalid User DN';
    },
    validate(value) {
      // consider checking this.fields[user].dirty before
      // running the ajax validation so that we don't validate a second time
      // on form submission if the field is already valid
      // we would need to save off the previous validation result somewhere
      // so that if this.fields[user].dirty is false, we just return the last validation result
      return new Promise((resolve) => {
        let isValid = false;

        axios.get(`/app/users/validDn/${encodeURIComponent(value)}`)
          .then((response) => {
            isValid = response.data;
            resolve({
              valid: isValid,
              data: undefined,
            });
          });
      });
    },
  });
</script>
