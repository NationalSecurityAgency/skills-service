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
    name: 'ToastSupport',
    methods: {
      successToast(toastTitle, msg) {
        this.toast(toastTitle, msg);
      },
      errorToast(toastTitle, msg) {
        this.toast(toastTitle, msg, 'danger');
      },
      toast(toastTitle, msg, toastVairant = 'success') {
        this.$bvToast.toast(msg, {
          title: toastTitle,
          autoHideDelay: 4000,
          toaster: 'b-toaster-top-center',
          solid: true,
          appendToast: true,
          variant: toastVairant,
        });
      },
    },
  };
</script>

<style scoped>

</style>
