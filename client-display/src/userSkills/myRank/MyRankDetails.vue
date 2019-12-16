<template>
    <section class='container mb-3'>
        <skills-spinner v-if='loading' :loading="loading" class="mt-5"></skills-spinner>

        <div v-if='!loading'>
            <skills-title>Rank Overview</skills-title>
            <div class='row text-center mt-2'>

                <my-rank-detail-stat-card
                        class="col-md-3 mb-2 mb-md-0"
                        icon-class="fas fa-users"
                        label="My Rank"
                        :value="myRankPosition"/>

                <my-rank-detail-stat-card
                        class="col-md-3 mb-2 mb-md-0"
                        icon-class="fas fa-trophy"
                        label="My Level"
                        :value="rankingDistribution.myLevel"/>

                <my-rank-detail-stat-card
                        class="col-md-3 mb-2 mb-md-0"
                        icon-class="fas fa-user-plus"
                        label="My Points"
                        :value="rankingDistribution.myPoints"/>

                <my-rank-detail-stat-card
                        class="col-md-3 mb-2 mb-md-0"
                        icon-class="fas fa-user-friends"
                        label="Total Users"
                        :value="totalNumUsers"/>

            </div>

            <div class="row mt-3">
                <div class="col-lg-6 mb-2 mb-lg-0">
                    <levels-breakdown-chart :users-per-level="usersPerLevel" :my-level="rankingDistribution.myLevel"/>
                </div>

                <div class="col-lg-6">
                    <my-rank-encouragement-card icon="fa fa-user-astronaut text-warning" class="mb-2">

                        <span v-if="rankingDistribution.pointsToPassNextUser === -1">
                            <h4 class="mb-2">Wow!! You are in the lead!</h4>
                            <div class="">That's one small step for man, one giant leap for mankind. </div>
                        </span>
                        <span v-else>
                            <h4 class="mb-2">Just <strong>{{rankingDistribution.pointsToPassNextUser | number }}</strong> more points...</h4>
                            <div class="">to pass the next participant. That's one small step for man, one giant leap for mankind. </div>
                        </span>

                    </my-rank-encouragement-card>

                    <my-rank-encouragement-card icon="fa fa-running text-danger" class="mb-2">
                         <span v-if="rankingDistribution.pointsAnotherUserToPassMe === -1">
                            <h4 class="mb-2">You just got started!!</h4>
                            <div class="">Exciting times, enjoy gaining those points!</div>
                        </span>
                        <span v-else>
                            <h4 class="mb-2">Rank may drop in 1, 2.., <strong>{{ rankingDistribution.pointsAnotherUserToPassMe | number }}</strong> points</h4>
                            <div class="">There is a competitor right behind you, only <strong>{{ rankingDistribution.pointsAnotherUserToPassMe | number }}</strong> points behind. Don't let them pass you!</div>
                        </span>
                    </my-rank-encouragement-card>

                    <my-rank-encouragement-card icon="fa fa-glass-cheers text-info">
                        <span v-if="myRank">
                            <span v-if="numUsersBehindMe <= 0">
                                <h4 class="mb-2">Earn those point riches!</h4>
                                <div class="">Earn skills and you will pass your fellow app users in no time!</div>
                            </span>
                            <span v-else>
                                <h4 class="mb-2"><strong>{{ numUsersBehindMe | number }}</strong> reasons to celebrate</h4>
                                <div class="">That's how many fellow app users have less points than you. Be Proud!!!</div>
                            </span>
                        </span>
                        <span v-else class="text-left" style="width: 4rem;">
                            <vue-simple-spinner size="medium" line-bg-color="#333" line-fg-color="#17a2b8" message="Get Excited! Results are on their way!"/>
                        </span>
                    </my-rank-encouragement-card>
                </div>

            </div>
        </div>
    </section>
</template>

<script>
    import Spinner from 'vue-simple-spinner';
    import MyRankDetailStatCard from '@/userSkills/myRank/MyRankDetailStatCard.vue';
    import LevelsBreakdownChart from '@/userSkills/myRank/LevelsBreakdownChart.vue';
    import MyRankEncouragementCard from '@/userSkills/myRank/MyRankEncouragementCard.vue';

    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    import SkillsTitle from '@/common/utilities/SkillsTitle.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

    export default {
        components: {
            SkillsSpinner,
            SkillsTitle,
            MyRankDetailStatCard,
            LevelsBreakdownChart,
            MyRankEncouragementCard,
            'vue-simple-spinner': Spinner,
        },
        props: {
            subjectId: String,
        },
        data() {
            return {
                loading: true,
                rankingDistribution: null,
                usersPerLevel: null,
                myRank: null,
            };
        },
        mounted() {
            this.getData();
        },
        methods: {
            getData() {
                this.loading = true;
                const subjectId = this.subjectId ? this.subjectId : null;
                UserSkillsService.getUserSkillsRankingDistribution(subjectId)
                    .then((response) => {
                        this.rankingDistribution = response;
                    })
                    .finally(() => {
                        this.loading = false;
                    });
                UserSkillsService.getRankingDistributionUsersPerLevel(subjectId)
                    .then((response) => {
                        this.usersPerLevel = response;
                    });
                UserSkillsService.getUserSkillsRanking(subjectId)
                    .then((response) => {
                        this.myRank = response;
                    });
            },
        },
        computed: {
            numUsersBehindMe() {
                return this.myRank ? this.myRank.numUsers - this.myRank.position : -1;
            },
            myRankPosition() {
                return this.myRank ? this.myRank.position : -1;
            },
            totalNumUsers() {
                return this.myRank ? this.myRank.numUsers : -1;
            },
        },
    };
</script>

<style scoped>
    ul {
        list-style: none;
        text-align: left;
        padding-left: 1rem;
    }

    ul li {
        padding-top: 1rem;
    }

    ul li i {
        font-size: 2rem;
        width: 2.5rem;
        text-align: center;
        vertical-align: middle;
    }

</style>
