<template>
  <div class="card">
    <div class="card-body p-0">
      <b-table :items="items" :fields="fields">
        <template #head(rank)="data">
            <i class="fas fa-sort-amount-up"></i> {{ data.label }}
        </template>
        <template v-slot:cell(rank)="data">
          #{{ data.value }}
        </template>

        <template #head(user)="data">
          <div class="text-left">
            <i class="far fa-user"></i> {{ data.label }}
          </div>
        </template>
        <template v-slot:cell(user)="data">
          <div class="text-left">
          {{ data.value }}
          </div>
        </template>

        <i class="fas fa-running"></i>
        <template #head(progress)="data">
          <div class="text-left">
            <i class="fas fa-running"></i> {{ data.label }}
          </div>
        </template>
        <template v-slot:cell(progress)="data">
          <div class="text-left">
            <b-badge>Level {{ data.item.level }}</b-badge>
            {{ data.item.points | number }} Points
          </div>
        </template>

        <template #head(firstVisit)="data">
            <i class="fas fa-running"></i> {{ data.label }}
        </template>
        <template v-slot:cell(firstVisit)="data">
            {{ data.value | relativeTime }}
        </template>

      </b-table>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'Leaderboard',
    data() {
      return {
        items: [
          {
            rank: 1,
            user: 'Dickerson Macdonald (blah)',
            points: 12203,
            level: 3,
            firstVisit: new Date().getTime(),
          },
          {
            rank: 2,
            user: 'Larsen Shaw (blah)',
            points: 11363,
            level: 3,
            firstVisit: new Date().getTime() - (1000 * 60 * 60 * 29),
          },
          {
            rank: 3,
            user: 'Geneva Wilson (blah)',
            points: 10473,
            level: 3,
            firstVisit: 39293923939,
          },
          {
            rank: 4,
            user: 'Jami Carney (blah)',
            points: 987,
            level: 3,
            firstVisit: 39293923939,
          },
          {
            rank: 5,
            user: 'Dickerson Junior (blah)',
            points: 864,
            level: 3,
            firstVisit: 39293923939,
          },
        ],
        fields: [
          {
            key: 'rank',
            label: 'Rank',
            sortable: false,
          },
          {
            key: 'user',
            label: 'User',
            sortable: false,
          },
          {
            key: 'progress',
            label: 'Progress',
            sortable: false,
          },
          {
            key: 'firstVisit',
            label: 'User Since',
            sortable: false,
          },
        ],
      };
    },
  };
</script>

<style scoped>

</style>
