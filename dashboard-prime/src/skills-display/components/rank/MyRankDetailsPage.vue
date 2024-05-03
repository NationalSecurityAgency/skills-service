<script setup>
import { useRoute } from 'vue-router'
import { computed, onMounted, ref } from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import LevelsBreakdownChart from '@/skills-display/components/rank/LevelsBreakdownChart.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const colors = useColors()
const numFormat = useNumberFormat()
const attributes = useSkillsDisplayAttributesState()

const rankingDistributionLoading = ref(true)
const rankingDistribution = ref({})

const usersPerLevelLoading = ref(true)
const usersPerLevel = ref({})

const myRankLoading = ref(true)
const myRank = ref({})

const loading = computed(() => {
  return rankingDistributionLoading.value || usersPerLevelLoading.value || myRankLoading.value
})

onMounted(() => {
  loadData()
})

const loadData = () => {
  const subjectId = route.params.subjectId || null
  skillsDisplayService.getUserSkillsRankingDistribution(subjectId)
    .then((response) => {
      rankingDistribution.value = response
    })
    .finally(() => {
      rankingDistributionLoading.value = false
    })
  skillsDisplayService.getRankingDistributionUsersPerLevel(subjectId)
    .then((response) => {
      usersPerLevel.value = response
    })
    .finally(() => {
      usersPerLevelLoading.value = false
    })
  skillsDisplayService.getUserSkillsRanking(subjectId)
    .then((response) => {
      myRank.value = response
    })
    .finally(() => {
      myRankLoading.value = false
    })
}

const myRankPosition = computed(() => {
  if (!myRank.value) {
    return -1
  }
  return myRank.value.optedOut ? -99 : myRank.value.position
})
const totalNumUsers = computed(() => {
  return myRank.value ? myRank.value.numUsers : -1
})

const numUsersBehindMe = computed(() => {
  return myRank.value ? myRank.value.numUsers - myRank.value.position : -1
})
</script>

<template>
  <div>
    <skills-spinner v-if="loading" :is-loading="loading" class="mt-5" />
    <div v-if="!loading">
      <skills-title>My Rank</skills-title>

      <div class='flex flex-wrap gap-3 mt-3'>

        <div class="w-min-13rem flex-1">
          <media-info-card
            :title="`${numFormat.pretty(myRankPosition)}`"
            class="h-full text-center font-bold"
            :icon-class="`fas fa-users ${colors.getTextClass(0)}`"
            data-cy="myRankPositionStatCard">
            <span class="uppercase font-normal">My Rank</span>
          </media-info-card>
        </div>

        <div class="w-min-13rem flex-1">
          <media-info-card
            :title="`${rankingDistribution.myLevel}`"
            class="h-full text-center font-bold"
            :icon-class="`fas fa-trophy ${colors.getTextClass(1)}`"
            data-cy="myRankLevelStatCard">
            <span class="uppercase font-normal">My {{ attributes.levelDisplayName }}</span>
          </media-info-card>
        </div>

        <div class="w-min-13rem flex-1">
          <media-info-card
            :title="`${numFormat.pretty(rankingDistribution.myPoints)}`"
            class="h-full text-center font-bold"
            :icon-class="`fas fa-user-plus ${colors.getTextClass(2)}`"
            data-cy="myRankPointsStatCard">
            <span class="uppercase font-normal">My Points</span>
          </media-info-card>
        </div>

        <div class="w-min-13rem flex-1">
          <media-info-card
            :title="`${numFormat.pretty(totalNumUsers)}`"
            class="h-full  text-center font-bold"
            :icon-class="`fas fa-user-friends ${colors.getTextClass(3)}`"
            data-cy="myRankTotalUsersStatCard">
            <span class="uppercase font-normal">Total Users</span>
          </media-info-card>
        </div>
      </div>

      <div class="flex gap-3 flex-wrap mt-3">
        <div class="flex-1 h-full">
          <levels-breakdown-chart :users-per-level="usersPerLevel"
                                  :my-level="rankingDistribution.myLevel" />
        </div>

        <div class="flex-1" data-cy="encouragementCards">

          <div class="h-full">
            <div class="flex flex-column gap-3 h-full">
              <div class="flex-1">
                <media-info-card v-if="rankingDistribution.pointsToPassNextUser === -1"
                                 title="You are in the lead!"
                                 class="h-full"
                                 :icon-class="`fas fa-user-friends ${colors.getTextClass(4)}`"
                                 data-cy="myRankTotalUsersStatCard">
                <span class="text-lg">That's one small step <i class="fas fa-shoe-prints" aria-hidden="true"></i> for man, one giant leap <i
                  class="fas fa-running" aria-hidden="true"></i> for mankind.</span>
                </media-info-card>
                <media-info-card v-else
                                 title="So close...."
                                 class="h-full"
                                 :icon-class="`fas fa-user-friends ${colors.getTextClass(4)}`"
                                 data-cy="myRankTotalUsersStatCard">
                  <span class="text-lg">Just
                  <Tag>{{ numFormat.pretty(rankingDistribution.pointsToPassNextUser) }}</Tag>
                  more points... to pass the next participant.</span>
                </media-info-card>
              </div>
              <div class="flex-1">
                <media-info-card v-if="rankingDistribution.pointsAnotherUserToPassMe === -1"
                                 title="You just got started!!"
                                 class="h-full"
                                 :icon-class="`fas fa-running ${colors.getTextClass(5)}`"
                                 data-cy="myRankTotalUsersStatCard">
                  <span class="text-lg">Exciting times, <i class="fas fa-rocket" aria-hidden="true"></i> enjoy gaining those points!</span>
                </media-info-card>
                <media-info-card v-else
                                 title="Your Rank may drop"
                                 class="h-full"
                                 :icon-class="`fas fa-running ${colors.getTextClass(5)}`"
                                 data-cy="myRankTotalUsersStatCard">
                 <span class="text-lg">There is a competitor right behind you, only
                  <Tag>{{ numFormat.pretty(rankingDistribution.pointsAnotherUserToPassMe) }}</Tag>
                  points behind. Don't let them pass you!</span>
                </media-info-card>
              </div>
              <div class="flex-1">
                <media-info-card v-if="numUsersBehindMe <= 0"
                                 title="Earn those point riches!"
                                 class="h-full"
                                 :icon-class="`fas fa-glass-cheers ${colors.getTextClass(6)}`"
                                 data-cy="myRankTotalUsersStatCard">
                  <span class="text-lg">Earn {{ attributes.skillDisplayName }} and you will pass your fellow users <i class="fas fa-user-tie" aria-hidden="true"></i> in no time!</span>
                </media-info-card>
                <media-info-card v-else
                                 :title="`${numFormat.pretty(numUsersBehindMe)} reasons to celebrate`"
                                 class="h-full"
                                 :icon-class="`fas fa-glass-cheers ${colors.getTextClass(6)}`"
                                 data-cy="myRankTotalUsersStatCard">
                  <span class="text-lg">That's how many fellow users have less points than you. Be Proud!!!</span>
                </media-info-card>
              </div>
            </div>
          </div>


          <!--          <my-rank-encouragement-card icon="fa fa-glass-cheers" :icon-color="infoCards().iconColors[2]">-->
          <!--                        <span v-if="myRank">-->
          <!--                            <span v-if="numUsersBehindMe <= 0">-->
          <!--                                <div class="h4 mb-2">Earn those point riches!</div>-->
          <!--                                <div class="">Earn {{ skillDisplayName }} and you will pass your fellow app users in no time!</div>-->
          <!--                            </span>-->
          <!--                            <span v-else>-->
          <!--                                <div class="h4 mb-2"><strong>{{ numUsersBehindMe | number }}</strong> reasons to celebrate</div>-->
          <!--                                <div class="">That's how many fellow app users have less points than you. Be Proud!!!</div>-->
          <!--                            </span>-->
          <!--                        </span>-->
          <!--            <span v-else class="text-left" style="width: 4rem;">-->
          <!--                            <vue-simple-spinner size="medium" line-bg-color="#333" line-fg-color="#17a2b8" message="Get Excited! Results are on their way!"/>-->
          <!--                        </span>-->
          <!--          </my-rank-encouragement-card>-->
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>

</style>