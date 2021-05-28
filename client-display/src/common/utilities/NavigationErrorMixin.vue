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
<script>
  export default {
    name: 'NavigationErrorMixin',
    methods: {
      handlePush(page) {
        this.$router.push(page).catch((error) => {
          if (error.message.indexOf('Redirected when going from') !== -1) {
            // squash, vue-router made changes in version 3 that
            // causes a redirect to trigger an error. router-link squashes these and in previous
            // versions of vue-router they were ignored. Because we define a beforeRouter guard
            // that updates the destination page to capture the previous route for back button navigation support
            // that causes a redirect which triggers this benign error
          } else {
            // eslint-disable-next-line
            console.error(error);
          }
        });
      },
      handleReplace(page) {
        this.$router.replace(page).catch((error) => {
          if (error.message.indexOf('Redirected when going from') !== -1) {
            // squash, vue-router made changes in version 3 that
            // causes a redirect to trigger an error. router-link squashes these and in previous
            // versions of vue-router they were ignored. Because we define a beforeRouter guard
            // that updates the destination page to capture the previous route for back button navigation support
            // that causes a redirect which triggers this benign error
          } else {
            // eslint-disable-next-line
            console.error(error);
          }
        });
      },
    },
  };
</script>
