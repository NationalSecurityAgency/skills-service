<template>
  <div class="box">
    <div class="columns">
      <div class="column">
        <div class="subtitle">CORS Settings</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="newAllowedOrigin" class="button is-outlined is-info">
          <span>Add New Origin</span>
          <span class="icon is-small">
            <i class="fas fa-plus-circle"/>
          </span>
        </a>
      </div>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="allowedOriginsContainer" enter-active-class="animated fadeIn">
        <v-client-table :data="data" :columns="columns" :options="options">
          <div slot="edit" slot-scope="props" class="field has-addons">
            <p class="control">
              <a v-on:click="updateAllowedOrigin(props.row)" class="button">
                <span class="icon is-small">
                  <i class="fas fa-edit"/>
                </span>
                <span>Edit</span>
              </a>
            </p>
            <p class="control">
              <a v-on:click="deleteAllowedOrigin(props.row)" class="button">
              <span class="icon is-small">
                <i class="fas fa-trash"/>
              </span>
                <span>Delete</span>
              </a>
            </p>
          </div>
        </v-client-table>
      </transition>
    </loading-container>
  </div>
</template>

<script>
  import axios from 'axios';
  import EditAllowedOrigin from './EditAllowedOrigin';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastHelper from '../utils/ToastHelper';

  export default {
    name: 'AllowedOrigins',
    components: { LoadingContainer },
    props: ['project'],
    data() {
      return {
        serverErrors: [],
        // allowed origins properties
        isLoading: true,
        data: [],
        columns: ['allowedOrigin', 'edit'],
        options: {
          headings: {
            allowedOrigin: 'Allowed Origin',
            edit: '',
          },
          columnsClasses: {
            edit: 'control-column',
          },
          sortable: ['allowedOrigin'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          filterable: false,
          skin: 'table is-striped is-fullwidth',
        },
      };
    },
    mounted() {
      axios.get(`/admin/projects/${this.project.projectId}/allowedOrigins`)
        .then((response) => {
          this.isLoading = false;
          this.data = response.data;
        })
        .catch((e) => {
          this.serverErrors.push(e);
      });
    },
    methods: {
      newAllowedOrigin() {
        this.$modal.open({
          parent: this,
          component: EditAllowedOrigin,
          hasModalCard: true,
          width: 1110,
          props: {
            projectId: this.project.projectId,
            action: 'Create',
          },
          events: {
            'allowed-origin-created': this.allowedOriginAdded,
          },
        });
      },
      allowedOriginAdded(allowedOrigin) {
        this.data.push(allowedOrigin);
        this.$toast.open(ToastHelper.defaultConf(`Created '${allowedOrigin.allowedOrigin}' allowed origin`));
      },
      updateAllowedOrigin(row) {
        this.$modal.open({
          parent: this,
          component: EditAllowedOrigin,
          hasModalCard: true,
          width: 1110,
          props: {
            projectId: this.project.projectId,
            allowedOrigin: row.allowedOrigin,
            id: row.id,
            action: 'Update',
          },
          events: {
            'allowed-origin-updated': this.allowedOriginUpdated,
          },
        });
      },
      allowedOriginUpdated(allowedOrigin) {
        this.data = this.data.filter(item => item.id !== allowedOrigin.id);
        this.data.push(allowedOrigin);
        this.$toast.open(ToastHelper.defaultConf(`Updated allowed orgin: '${allowedOrigin.allowedOrigin}'`));
      },
      deleteAllowedOrigin(row) {
        this.$dialog.confirm({
          title: 'Delete Allowed Origin',
          message: `Are you absolutely sure you want to delete origin [${row.allowedOrigin}]?`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          onConfirm: () => this.deleteAllowedOriginAjax(row),
        });
      },
      deleteAllowedOriginAjax(row) {
        axios.delete(`/admin/projects/${row.projectId}/allowedOrigins/${encodeURIComponent(row.id)}`)
          .then(() => {
            this.data = this.data.filter(item => item.id !== row.id);
            this.$toast.open(ToastHelper.defaultConf(`Removed allowed origin: '${row.allowedOrigin}'`));
        });
      },
    },
  };
</script>

<style type="scss">
  .control-column {
    width: 4rem;
  }

</style>
