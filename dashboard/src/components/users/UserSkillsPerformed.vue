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
<template>
  <div>
    <sub-page-header title="Performed Skills"/>

    <simple-card>
      <v-server-table class="vue-table-2" ref="table" :columns="columns" :url="getUrl()" :options="options"
                      @loaded="onLoaded" @loading="onLoading" v-on:error="emit('error', $event)">
        <server-table-loading-mask v-if="isLoading" slot="afterBody" />
        <div slot="performedOn" slot-scope="props">
          {{ getDate(props.row) }}
        </div>

        <div slot="delete" slot-scope="props">
          <b-button @click="deleteSkill(props.row)" variant="outline-hc"><i class="fas fa-trash"/></b-button>
        </div>
      </v-server-table>
    </simple-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ToastSupport from '../utils/ToastSupport';
  import UsersService from './UsersService';
  import ServerTableLoadingMask from '../utils/ServerTableLoadingMask';

  const { mapActions } = createNamespacedHelpers('users');

  export default {
    name: 'UserSkillsPerformed',
    mixins: [MsgBoxMixin, ToastSupport],
    components: {
      SimpleCard,
      SubPageHeader,
      ServerTableLoadingMask,
    },
    data() {
      return {
        displayName: 'Skills Performed Table',
        isLoading: true,
        data: [],
        columns: ['skillId', 'performedOn', 'delete'],
        options: {
          headings: {
            skillId: 'Skill ID',
            performedOn: 'Performed On',
            delete: '',
          },
          sortable: ['skillId', 'performedOn'],
          orderBy: {
            column: 'performedOn',
            ascending: false,
          },
          dateColumns: ['performedOn'],
          dateFormat: 'YYYY-MM-DD HH:mm',
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: true,
          highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
        },
        projectId: null,
        userId: null,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.userId = this.$route.params.userId;
    },
    methods: {
      ...mapActions([
        'loadUserDetailsState',
      ]),
      onLoading() {
        this.isLoading = true;
      },
      onLoaded(event) {
        this.isLoading = false;
        this.$emit('loaded', event);
      },
      getUrl() {
        return `/admin/projects/${this.projectId}/performedSkills/${this.userId}`;
      },
      emit(name, event) {
        this.$emit(name, event, this);
      },
      clear() {
        this.$refs.table.data = [];
        this.$refs.table.count = 0;
      },
      getDate(row) {
        return window.moment(row.performedOn)
          .format('LLL');
      },
      deleteSkill(row) {
        this.msgConfirm(`Removing skill [${row.skillId}] performed on [${this.getDate(row)}]. This will permanently remove this user's performed skill and cannot be undone.`)
          .then((res) => {
            if (res) {
              this.doDeleteSkill(row);
            }
          });
      },
      doDeleteSkill(skill) {
        this.isLoading = true;
        UsersService.deleteSkillEvent(this.projectId, skill, this.userId)
          .then((data) => {
            if (data.success) {
              // server table must not manually remove items but rather refresh the table
              this.$refs.table.refresh();
              this.loadUserDetailsState({ projectId: this.projectId, userId: this.userId });
              this.successToast('Removed Skill', `Skill '${skill.skillId}' was removed.`);
            } else {
              this.errorToast('Unable to Remove Skill', `Skill '${skill.skillId}' was not removed.  ${data.explanation}`);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
  .vue-table-2 table {
    width: 100%;
    position: relative;
  }
</style>
