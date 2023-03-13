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
    <sub-page-header title="Access"/>
    <b-card body-class="mb-4 p-0">
      <skills-spinner v-if="initialLoad" :is-loading="initialLoad"/>

      <div v-if="!initialLoad">
        <div class="row py-4 px-3">
          <div class="col">
            <b-overlay :show="table.options.busy"
                       variant="transparent"
                       spinner-variant="info"
                       spinner-type="grow"
                       spinner-small>
              <existing-user-input :suggest="true"
                                   ref="existingUserInput"
                                 :validate="true"
                                 user-type="DASHBOARD"
                                 :excluded-suggestions="userIds"
                                 v-model="selectedUser"
                                 data-cy="existingUserInput"/>
            </b-overlay>
          </div>
          <div class="col-auto">
            <b-button variant="outline-hc"
                      ref="addUserBtn"
                      @click="addUserRole"
                      aria-label="Add selected user as an admin of this quiz or survey"
                      :disabled="!userSelected || table.options.busy"
                      data-cy="addUserBtn">
              Add User <i class="fas fa-arrow-circle-right" aria-hidden="true"></i>
            </b-button>
          </div>
        </div>

        <skills-b-table id="quizUserRoleTable"
                        :options="table.options"
                        :items="userRoles"
                        tableStoredStateId="quizUserRoleTable"
                        data-cy="quizUserRoleTable">
          <template #head(userIdForDisplay)="data">
            <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
          </template>

          <template v-slot:cell(userIdForDisplay)="data">
            <div class="row" :data-cy="`quizAdmin_${data.item.userId}`">
              <div class="col">
                {{ data.value }}
              </div>
              <div class="col-auto">
                <b-tooltip target="warningIconForSelfRemoval" triggers="hover">
                  Can not remove <b>myself</b>. Sorry!!
                </b-tooltip>
                <i id="warningIconForSelfRemoval" v-if="!notCurrentUser(data.item.userId)"
                   data-cy="cannotRemoveWarning"
                   class="text-warning fas fa-exclamation-circle mr-1" />
                <b-button :ref="`delBtn_${data.item.userId}`"
                          @click="deleteUserRoleConfirm(data.item)"
                          :disabled="!notCurrentUser(data.item.userId)"
                          variant="outline-primary"
                          :aria-label="`remove access role from user ${data.item.userId}`"
                          data-cy="removeUserBtn">
                  <i class="text-warning fas fa-trash" aria-hidden="true"/>
                </b-button>
              </div>
            </div>
          </template>
        </skills-b-table>
      </div>
    </b-card>

    <removal-validation v-if="removeRoleInfo.showDialog"
                        v-model="removeRoleInfo.showDialog"
                        @do-remove="doDeleteUserRole"
                        @hidden="focusOnRefId(`delBtn_${removeRoleInfo.userInfo.userId}`)">
      This action will permanently remove <b>{{ removeRoleInfo.userInfo.userIdForDisplay }}</b> from having admin privileges.
    </removal-validation>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import QuizService from '@/components/quiz/QuizService';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import ExistingUserInput from '@/components/utils/ExistingUserInput';
  import RemovalValidation from '@/components/utils/modal/RemovalValidation';

  export default {
    name: 'QuizAccessPage',
    components: {
      ExistingUserInput,
      SubPageHeader,
      SkillsSpinner,
      SkillsBTable,
      RemovalValidation,
    },
    data() {
      return {
        initialLoad: true,
        quizId: this.$route.params.quizId,
        userRoles: [],
        userIds: [],
        removeRoleInfo: {
          showDialog: false,
          userInfo: {},
        },
        selectedUser: null,
        table: {
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'started',
            sortDesc: true,
            fields: [
              {
                key: 'userIdForDisplay',
                label: 'Quiz Admin',
                sortable: true,
              },
            ],
            pagination: {
              server: false,
              currentPage: 1,
              totalRows: 0,
              pageSize: 5,
              possiblePageSizes: [5, 10, 20, 50],
            },
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      userSelected() {
        return this.selectedUser && this.selectedUser.userId;
      },
    },
    methods: {
      loadData() {
        return QuizService.getQuizUserRoles(this.quizId)
          .then((res) => {
            this.table.options.pagination.totalRows = this.userRoles.length;
            this.userRoles = res;
            this.userIds = this.userRoles.map((u) => [u.userId, u.userIdForDisplay]).flatten();
            this.table.options.busy = false;
          })
          .finally(() => {
            this.initialLoad = false;
          });
      },
      deleteUserRoleConfirm(user) {
        this.removeRoleInfo.userInfo = user;
        this.removeRoleInfo.showDialog = true;
      },
      doDeleteUserRole() {
        this.table.options.busy = true;
        const { userIdForDisplay, userId } = this.removeRoleInfo.userInfo;
        QuizService.deleteQuizAdmin(this.quizId, userId)
          .finally(() => {
            this.loadData()
              .finally(() => {
                this.focusOnRefId('existingUserInput');
                this.$nextTick(() => {
                  this.$announcer.polite(`Admin ${userIdForDisplay} was removed`);
                });
              });
          });
      },
      notCurrentUser(userId) {
        return this.$store.getters.userInfo && userId !== this.$store.getters.userInfo.userId;
      },
      addUserRole() {
        this.table.options.busy = true;
        const { userIdForDisplay, userId } = this.selectedUser;
        QuizService.addQuizAdmin(this.quizId, userId)
          .then(() => {
            this.selectedUser = null;
            this.loadData()
              .then(() => {
                this.focusOnTable();
                this.$nextTick(() => {
                  this.$announcer.polite(`New admin ${userIdForDisplay} was added`);
                });
              });
          });
      },
      focusOnRefId(refId) {
        this.$nextTick(() => {
          const ref = this.$refs[refId];
          if (ref) {
            ref.focus();
          }
        });
      },
      focusOnTable() {
        this.$nextTick(() => {
          const quizUserRoleTable = document.getElementById('quizUserRoleTable');
          if (quizUserRoleTable) {
            const foundInput = quizUserRoleTable.querySelector('table thead th:first-child');
            if (foundInput) {
              quizUserRoleTable.focus();
            }
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>
