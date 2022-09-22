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
  import TableStateUtil from '../TableStateUtil';

  export default {
    name: 'PersistedSortMixin',
    data() {
      return {
        sortBy: '',
        sortDesc: false,
      };
    },
    mounted() {
      if (this.tableId) {
        const sorting = TableStateUtil.loadTableState(this.tableId);
        if (sorting) {
          this.sortBy = sorting.sortBy;
          this.sortDesc = sorting.sortDesc;
        }
      }
    },
    methods: {
      sortingChanged(ctx) {
        if (this.tableId) {
          TableStateUtil.saveTableSortState(this.tableId, ctx.sortBy, ctx.sortDesc);
        }

        // ctx.sortBy   ==> Field key for sorting by (or null for no sorting)
        // ctx.sortDesc ==> true if sorting descending, false otherwise
        this.currentPageInternal = 1;
        this.$emit('sort-changed', ctx);
      },
    },
  };
</script>
