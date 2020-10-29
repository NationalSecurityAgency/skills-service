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
    <b-modal id="searchProjectsModal" title="Pin Projects" v-model="show" :no-close-on-backdrop="true"
           @close="done" header-bg-variant="info" header-text-variant="light" no-fade body-class="px-0 mx-0">
      <b-container fluid class="px-0" data-cy="pinProjects">
        <b-row class="px-3">
          <b-col class="mx-0 pr-0">
            <b-input-group>
              <template #append>
                <b-button variant="outline-secondary" @click="searchValue=''" data-cy="pinProjectsClearSearch"><i class="fas fa-times"></i></b-button>
              </template>
              <b-input type="search" v-model="searchValue" placeholder="Search projects to pin" data-cy="pinProjectsSearchInput"></b-input>
            </b-input-group>
          </b-col>
          <b-col md="auto">
            <span class="text-secondary">OR</span> <b-button variant="outline-primary" @click="loadAll"  data-cy="pinProjectsLoadAllButton">Load All <i class="fas fa-weight-hanging text-muted"></i></b-button>
          </b-col>
        </b-row>
        <div style="min-height: 6rem;">
          <skills-spinner :is-loading="isLoading" />
          <div v-if="!isLoading" class="mt-4">
            <div v-if="hasResults">
              <b-table striped hover :items="result.values" :fields="result.fields"
                       :per-page="result.paging.perPage"
                       :current-page="result.paging.currentPage"
                       data-cy="pinProjectsSearchResults">
                <template #cell(controls)="data">
                  <b-button v-if="!data.item.pinned" @click="pinProject(data.item)" variant="outline-primary" size="sm"
                            data-cy="pinButton">
                    <i class="fas fa-thumbtack"/>
                  </b-button>
                  <b-button v-if="data.item.pinned" variant="outline-success" size="sm" disabled data-cy="pinedButtonIndicator">
                    <i class="fas fa-check"/>
                  </b-button>
                </template>
                <template #cell(totalPoints)="data">
                  {{ data.value | number }}
                </template>
              </b-table>
              <b-row align-h="center" class="px-3">
                <b-col>

                </b-col>
                <b-col cols="6">
                  <b-pagination
                  v-model="result.paging.currentPage"
                  :total-rows="result.values.length"
                  :per-page="result.paging.perPage"
                  align="fill"
                  size="sm"
                  data-cy="pinedResultsPaging"/>
                </b-col>
                <b-col>
                  <div class="small text-right" data-cy="pinProjectsSearchResultsNumRows">
                    <span class="text-muted">Rows:</span> {{ result.values.length | number}}
                  </div>
                </b-col>
              </b-row>
            </div>
            <div v-if="!hasResults && !hasSearch" class="text-center">
              <i class="fas fa-2x fa-th-list text-secondary"></i>
              <div class="h4 mt-2 text-secondary">
                Search Project Catalog
              </div>
              <p class="small">
                Only projects that have not been pinned are returned.
              </p>
            </div>
            <div v-if="!hasResults && hasSearch" class="text-center">
              <i class="fas fa-2x fa-dragon text-secondary"></i>
              <div class="h4 mt-2 text-secondary">
                No Results
              </div>
              <p class="small">
                Only projects that have not been pinned are returned.
              </p>
            </div>
          </div>
        </div>
      </b-container>

      <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="done" data-cy="modalDoneButton">
          Done
        </b-button>
      </div>
    </b-modal>
</template>

<script>
  import ProjectService from './ProjectService';
  import SettingsService from '../settings/SettingsService';
  import SkillsSpinner from '../utils/SkillsSpinner';

  export default {
    name: 'PinProjects',
    components: { SkillsSpinner },
    props: {
      show: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        searchValue: '',
        result: {
          values: [],
          paging: {
            totalRows: 1,
            currentPage: 1,
            perPage: 5,
            pageOptions: [5, 10, 15],
          },
          fields: [{
                     key: 'name',
                     sortable: true,
                   },
                   {
                     key: 'numSkills',
                     label: 'Skills',
                     sortable: false,
                   },
                   {
                     key: 'totalPoints',
                     label: 'Points',
                     sortable: false,
                   }, {
                     key: 'controls',
                     sortable: false,
                     label: '',
                   }],
        },
      };
    },
    watch: {
      searchValue(newValue) {
        this.searchData(newValue);
      },
    },
    computed: {
      hasResults() {
        return this.result.values && this.result.values.length > 0;
      },
      hasSearch() {
        return this.searchValue && this.searchValue.length > 0;
      },
    },
    methods: {
      done() {
        this.$emit('done');
        this.searchValue = '';
        this.result.values = [];
      },
      searchData(searchValue) {
        if (!searchValue) {
          this.result.values = [];
        } else {
          this.isLoading = true;
          ProjectService.searchProjects(searchValue)
            .then((response) => {
              this.result.values = response.filter((item) => !item.pinned);
            })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      loadAll() {
        this.searchValue = '';
        this.isLoading = true;
        ProjectService.loadAllProjects()
          .then((response) => {
            this.result.values = response.filter((item) => !item.pinned);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      pinProject(item) {
        const itemRef = item;
        SettingsService.pinProject(item.projectId)
          .then(() => {
            itemRef.pinned = true;
          });
      },
    },
  };
</script>

<style lang="scss" scoped>

</style>
