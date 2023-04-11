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
  <div class="container-fluid">
    <sub-page-header title="Manage My Projects" class="pt-4">
      <b-button :to="{ name: 'MyProgressPage' }"
                data-cy="backToProgressAndRankingBtn"
                variant="outline-primary"><i class="fas fa-arrow-alt-circle-left" aria-hidden="true"/> Back <span class="d-none d-md-inline">to Progress and Ranking</span></b-button>
    </sub-page-header>

    <skills-spinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <div v-if="hasProjects">
        <b-row class="mb-3">
          <b-col cols="12" xl="7" order-xl="2">
            <b-row>
              <b-col cols="12" md="6" lg="4" class="mt-1">
                <media-info-card :title="`${counts.all}`" sub-title="ALL PROJECTS" icon-class="fas fa-globe text-success"
                                 data-cy="allProjectsCount">
                </media-info-card>
              </b-col>
              <b-col cols="12" md="6" lg="4" class="mt-1">
                <media-info-card :title="`${counts.myProjects}`" sub-title="MY PROJECTS"
                                 icon-class="fas fa-heart text-danger"
                                 data-cy="myProjectCount">
                </media-info-card>
              </b-col>
              <b-col cols="12" lg="4" class="mt-1">
                <media-info-card :title="`${counts.discoverProjects}`" sub-title="DISCOVER NEW"
                                 icon-class="fas fa-search text-warning"
                                 data-cy="discoverNewProjCount">
                </media-info-card>
              </b-col>
            </b-row>
          </b-col>
          <b-col cols="12" xl="5" order-xl="1" class="mt-3">
            <b-form-group
              id="searchProjectsFormGroup"
              label="Search:"
              label-for="searchProjectsInput"
            >
              <b-input-group>
                <template #append>
                  <b-button variant="outline-secondary"
                            @click="searchByName('')"
                            data-cy="clearSearch"
                            aria-label="clear search button"><i class="fas fa-times" aria-hidden="true"/></b-button>
                </template>
                <b-input id="searchProjectsInput" v-focus v-model="searchValue" @input="searchByName" data-cy="searchInput"
aria-label="search for projects to pin"></b-input>
              </b-input-group>
            </b-form-group>
          </b-col>
        </b-row>
        <b-card body-class="p-0" class="mb-3">
          <b-table striped
                 bordered
                 stacked="md"
                 :items="projects"
                 :fields="fields"
                 :per-page="paging.perPage"
                 :current-page="paging.currentPage"
                 :sort-by.sync="sortBy"
                 :sort-desc.sync="sortDesc"
                 @sort-changed="sortingChanged"
                 :no-sort-reset="true"
                 :show-empty="true"
                 aria-label="Projects"
                 data-cy="discoverProjectsTable">
          <template #head(name)="data">
            <span class="text-primary"><i class="fas fa-list-alt skills-color-projects" /> {{ data.label }}</span>
          </template>
          <template #head(isMyProject)="data">
            <span class="text-primary"><i class="fas fa-heart text-danger" /> {{ data.label }}</span>
          </template>
          <template #head(numSkills)="data">
            <span class="text-primary"><i class="fas fa-graduation-cap skills-color-skills" /> {{ data.label }}</span>
          </template>
          <template #head(numSubjects)="data">
            <span class="text-primary"><i class="fas fa-cubes skills-color-subjects" /> {{ data.label }}</span>
          </template>
          <template #head(numBadges)="data">
            <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points" /> {{ data.label }}</span>
          </template>
          <template #head(totalPoints)="data">
            <span class="text-primary"><i class="fas fa-award skills-color-badges" /> {{ data.label }}</span>
          </template>

          <template #cell(name)="data">
            <span v-if="data.item.hasDescription">
                <b-button size="sm" @click="data.toggleDetails" variant="outline-info"
                          class="mr-2 py-0 px-1"
                          :aria-label="`Show description for ${data.item.name}`"
                          :data-cy="`expandDetailsBtn_${data.item.projectId}`">
                  <i v-if="data.detailsShowing" class="fas fa-caret-up"/>
                  <i v-else class="fas fa-caret-down"/>
                </b-button>
              </span>
            <div class="row">
            <div class="col">
              <span v-if="data.item.nameHtml" v-html="data.item.nameHtml"></span>
              <span v-else>{{ data.item.name }}
              </span>
              </div>
              <div class="col-auto">
                <b-button v-if="isEmailEnabled" variant="outline-primary" style="float:right" :aria-label="`Contact ${data.item.name} project owner`"
                                 @click="showContactOwner" :data-cy="`contactOwnerBtn_${ data.item.projectId }`">
                        Contact Project <i aria-hidden="true" class="fas fas fa-mail-bulk"/>
                 </b-button>
              </div>
                <contact-owners-dialog v-if="showContact" :project-name="`${data.item.name}`" v-model="showContact" :project-id="data.item.projectId"/>
             </div>
          </template>

          <template #cell(isMyProject)="data">
            <div v-if="data.value">
              <b-badge  class="animate__bounceIn" variant="success"><i class="fas fa-heart" /> My Project</b-badge>
                <b-button @click="removeFromMyProjects(data.item)" variant="outline-info" class="ml-2"
                          size="sm"
                          data-cy="removeBtn"
                          :aria-label="`remove project ${data.item.projectId} from my projects`">
                  <b-spinner small v-if="data.item.loading"></b-spinner>
                  <i class="fas fa-times-circle" />
                </b-button>
            </div>
            <div v-else>
              <b-button @click="addToMyProjects(data.item) " variant="outline-primary" class="animate__bounceIn"
                        size="sm"
                        data-cy="addButton"
                        :aria-label="`add project ${data.item.projectId} to my projects`">
                <b-spinner small v-if="data.item.loading"></b-spinner>
                <span  v-if="!data.item.loading"><i class="fas fa-plus-circle" style="width: 1rem;" aria-hidden="true"/> Add</span>
              </b-button>
            </div>
          </template>
          <template #cell(numSkills)="data">
            {{ data.value | number }}
          </template>
          <template #cell(numSubjects)="data">
            {{ data.value | number }}
          </template>
          <template #cell(numBadges)="data">
            {{ data.value | number }}
          </template>
          <template #cell(totalPoints)="data">
            {{ data.value | number }}
          </template>

          <template #empty="scope">
            <no-content2 class="my-4" :title="scope.emptyText" :message="`Please modify your search string: [${searchValue}]`" />
          </template>

          <template #row-details="row">
            <div class="row">
              <div class="col-12 pl-5 mt-2 mb-2 pr-5">
                <project-description-row :project-id="row.item.projectId" />
              </div>
            </div>
          </template>
        </b-table>
          <b-row align-h="center" class="mt-3">
          <b-col>

          </b-col>
          <b-col cols="12" sm="auto">
            <b-pagination
              v-model="paging.currentPage"
              :total-rows="paging.totalRows"
              :per-page="paging.perPage"
              align="fill"
              pills
              data-cy="discoverProjectsPaging"/>
          </b-col>
          <b-col>
            <div class="small text-right px-3" data-cy="projectsTableTotalRows">
              <span class="text-muted">Rows:</span> {{ paging.totalRows | number}}
            </div>
          </b-col>
        </b-row>
        </b-card>
      </div>
      <div v-if="!hasProjects">
        <no-projects-in-prod-message />
      </div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex';
  import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog';
  import ProjectDescriptionRow from '@/components/myProgress/discover/ProjectDescriptionRow';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import ProjectService from '../../projects/ProjectService';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import MediaInfoCard from '../../utils/cards/MediaInfoCard';
  import NoContent2 from '../../utils/NoContent2';
  import PersistedSortMixin from '../../utils/table/PersistedSortMixin';
  import NoProjectsInProdMessage from './NoProjectsInProdMessage';

  export default {
    name: 'DiscoverProjectsPage',
    mixins: [PersistedSortMixin],
    components: {
      NoProjectsInProdMessage,
      NoContent2,
      MediaInfoCard,
      SkillsSpinner,
      SubPageHeader,
      ProjectDescriptionRow,
      ContactOwnersDialog,
    },
    props: [],
    mounted() {
      this.loadAll();
    },
    data() {
      return {
        isLoading: true,
        showContact: false,
        searchValue: '',
        projects: [],
        originalProjects: [],
        tableStoredStateId: 'DiscoverProjects',
        counts: {
          all: 0,
          myProjects: 0,
          discoverProjects: 0,
        },
        fields: [
          {
            key: 'name',
            label: 'Project',
            sortable: true,
          },
          {
            key: 'isMyProject',
            label: 'My Project',
            sortable: true,
          },
          {
            key: 'numSubjects',
            label: 'Subjects',
            sortable: true,
          },
          {
            key: 'numSkills',
            label: 'Skills',
            sortable: true,
          },
          {
            key: 'numBadges',
            label: 'Badges',
            sortable: true,
          },
          {
            key: 'totalPoints',
            label: 'Points',
            sortable: true,
          }],
        paging: {
          totalRows: 1,
          currentPage: 1,
          perPage: 6,
          pageOptions: [5, 10, 15],
        },
      };
    },
    computed: {
      hasProjects() {
        return this.originalProjects && this.originalProjects.length > 0;
      },
      ...mapGetters([
         'isEmailEnabled',
      ]),
    },
    methods: {
      loadAll() {
        this.searchValue = '';
        this.isLoading = true;
        ProjectService.getAvailableForMyProjects()
          .then((response) => {
            this.originalProjects = response.map((item) => ({ loading: false, ...item }));
            // need a shallow copy
            this.projects = this.originalProjects.map((item) => item);
            this.paging.totalRows = this.projects.length;
            this.updateCounts();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      addToMyProjects(item) {
        const itemRef = item;
        itemRef.loading = true;
        ProjectService.addToMyProjects(item.projectId)
          .then(() => {
            itemRef.isMyProject = true;
            this.updateCounts();
          }).finally(() => {
            itemRef.loading = false;
          }).then(() => {
            this.$nextTick(() => this.$announcer.polite(`${item.name} has been added to my projects`));
          });
      },
      removeFromMyProjects(item) {
        const itemRef = item;
        itemRef.loading = true;
        ProjectService.removeFromMyProjects(item.projectId)
          .then(() => {
            itemRef.isMyProject = false;
            this.updateCounts();
          }).finally(() => {
            itemRef.loading = false;
          }).then(() => {
            this.$nextTick(() => this.$announcer.polite(`${item.name} has been removed from my projects`));
          });
      },
      updateCounts() {
        this.counts.all = this.originalProjects.length;
        this.counts.myProjects = this.originalProjects.filter((item) => item.isMyProject).length;
        this.counts.discoverProjects = this.counts.all - this.counts.myProjects;
      },
      searchByName(searchString) {
        this.paging.currentPage = 1;
        this.searchValue = searchString;

        if (!searchString || searchString === '') {
          this.projects = this.originalProjects.map((item) => Object.assign(item, { nameHtml: null }));
        } else {
          const searchStrNormalized = searchString.trim().toLowerCase();
          const foundItems = this.originalProjects.filter((item) => item.name?.trim()?.toLowerCase().includes(searchStrNormalized));
          this.projects = foundItems.map((item) => {
            const theName = item.name;
            const index = theName.toLowerCase().indexOf(searchStrNormalized);
            const nameHtml = `${theName.substring(0, index)}<mark>${theName.substring(index, index + searchStrNormalized.length)}</mark>${theName.substring(index + searchStrNormalized.length)}`;
            return Object.assign(item, { nameHtml });
          });
          const matchCount = this.projects.length;
          this.$nextTick(() => this.$announcer.polite(`projects filtered by ${searchString}, there ${matchCount > 1 ? 'are' : 'is'} ${matchCount} matching project${(matchCount > 1 || matchCount === 0) ? 's' : ''}`));
        }
        this.paging.totalRows = this.projects.length;
      },
      showContactOwner() {
        this.showContact = true;
      },
    },
  };
</script>

<style scoped>
  hr {
    border:none;
    height: 20px;
    width: 90%;
    height: 50px;
    border-bottom: 1px solid rgba(45, 135, 121, 0.31);
    box-shadow: 0 10px 10px -10px rgba(45, 134, 120, 0.15);
    margin: -50px auto 10px;
    width: 90%;
  }
</style>
