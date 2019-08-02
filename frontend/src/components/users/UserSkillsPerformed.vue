<template>
  <div>
    <sub-page-header title="Performed Skills"/>

    <simple-card>
      <v-server-table class="vue-table-2" ref="table" :columns="columns" :url="getUrl()" :options="options"
                      v-on:loaded="emit('loaded', $event)" v-on:error="emit('error', $event)">
        <div slot="performedOn" slot-scope="props">
          {{ getDate(props.row) }}
        </div>

        <div slot="delete" slot-scope="props">
          <b-button @click="deleteSkill(props.row)" variant="outline-primary"><i class="fas fa-trash"/></b-button>
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

  const { mapActions } = createNamespacedHelpers('users');

  export default {
    name: 'UserSkillsPerformed',
    mixins: [MsgBoxMixin, ToastSupport],
    components: {
      SimpleCard,
      SubPageHeader,
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
        UsersService.deleteSkillEvent(this.projectId, skill)
          .then((data) => {
            if (data.success) {
              const index = this.$refs.table.data.findIndex(item => item.id === skill.id);
              this.$refs.table.data.splice(index, 1);
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

</style>
