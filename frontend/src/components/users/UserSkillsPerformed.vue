<template>
  <div class="box">
    <div class="columns">
      <div class="column">
        <div class="subtitle">Skills Performed</div>
      </div>
    </div>
    <v-server-table ref="table" :columns="columns" :url="getUrl()" :options="options"
                    v-on:loaded="emit('loaded', $event)" v-on:error="emit('error', $event)">
      <div slot="performedOn" slot-scope="props" class="field has-addons">
        {{ getDate(props) }}
      </div>
    </v-server-table>
  </div>
</template>

<script>

  export default {
    name: 'UserSkillsPerformed',
    components: { },
    props: ['projectId', 'userId'],
    data() {
      return {
        displayName: 'Skills Performed Table',
        isLoading: true,
        data: [],
        columns: ['skillId', 'performedOn'],
        options: {
          headings: {
            skillId: 'Skill ID',
            performedOn: 'Performed On',
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
      };
    },
    methods: {
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
      getDate(props) {
        return window.moment(props.row.performedOn).format('LLL');
      },
    },
  };
</script>

<style scoped>

</style>
