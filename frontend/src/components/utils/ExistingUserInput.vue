<template>
  <div class="existingUserInput">
    <b-field :label="fieldLabel">
      <b-autocomplete
        data-vv-name="user"
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
  </div>
</template>

<script>
  import axios from 'axios';
  import debounce from 'lodash.debounce';

  export default {
    name: 'ExistingUserInput',
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
</script>
