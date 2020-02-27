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
    name: 'MsgBoxMixin',
    methods: {
      msgConfirm(msg, overallTitle = 'Please Confirm!', okButtonTitle = 'YES, Delete It!') {
        return this.$bvModal.msgBoxConfirm(msg, {
          title: overallTitle,
          size: 'md',
          buttonSize: 'sm',
          okVariant: 'danger',
          okTitle: okButtonTitle,
          cancelTitle: 'Cancel',
          footerClass: 'p-2',
          hideHeaderClose: false,
          centered: true,
        });
      },
      msgOk(msg, overallTitle = 'Message!', okButtonTitle = 'Ok') {
        return this.$bvModal.msgBoxOk(msg, {
          title: overallTitle,
          size: 'sm',
          buttonSize: 'sm',
          okTitle: okButtonTitle,
          footerClass: 'p-2',
          hideHeaderClose: false,
          centered: true,
        });
      },
    },
  };
</script>

<style scoped>

</style>
