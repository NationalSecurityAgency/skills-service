<script setup>
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import { computed, onMounted, ref } from 'vue'
import MyBadgesDetails from '@/skills-display/components/badges/MyBadgesDetails.vue'
import BadgesCatalog from '@/skills-display/components/badges/BadgesCatalog.vue'
import MyProgressTitle from '@/components/myProgress/MyProgressTitle.vue'

const loading = ref(true)
const badges = ref([])

onMounted(() => {
  loadBadges()
})

const unachievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === false))
const achievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === true))


const loadBadges = () => {
  loading.value = true
  MyProgressService.loadMyBadges().then((res) => {
    badges.value = res
  }).finally(() => {
    loading.value = false
  })
}
</script>

<template>
<div>
  <my-progress-title title="My Badges" />

  <my-badges-details
    data-cy="achievedBadges"
    :badges="achievedBadges"
    class="mt-3"
  />

  <badges-catalog class="mt-3"
                  :badges="unachievedBadges"
                  data-cy="availableBadges">
  </badges-catalog>
</div>
</template>

<style scoped>

</style>