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
                        :value="rankingDistribution.myPosition"/>

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
                        :value="rankingDistribution.totalUsers"/>

            </div>

            <div class="row mt-3">
                <div class="col-lg-6 mb-2 mb-lg-0">
                    <levels-breakdown-chart :ranking-distribution="rankingDistribution"/>
                </div>

                <div class="col-lg-6">
                    <my-rank-encouragement-card icon="fa fa-user-astronaut text-warning" class="mb-2">

                        <span v-if="rankingDistribution.pointsToPassNextUser === -1">
                            <h4 class="mb-2">Wow!! You are in the lead!</h4>
                            <div class="">That's one small step for man, one giant leap for mankind. </div>
                        </span>
                        <span v-else>
                            <h4 class="mb-2">Just <strong>{{rankingDistribution.pointsToPassNextUser}}</strong> more points...</h4>
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

                    <my-rank-encouragement-card icon="fa fa-glass-cheers text-primary">
                        <span v-if="numUsersBehindMe <= 0">
                            <h4 class="mb-2">Earn those point riches!</h4>
                            <div class="">Earn skills and you will pass your fellow app users in no time!</div>
                        </span>
                        <span v-else>
                            <h4 class="mb-2"><strong>{{ numUsersBehindMe }}</strong> reasons to celebrate</h4>
                            <div class="">That's how many fellow app users have less points than you. Be Proud!!!</div>
                        </span>
                    </my-rank-encouragement-card>
                </div>

            </div>
        </div>
    </section>
</template>

<script>
    import MyRankDetailStatCard from '@/userSkills/myRank/MyRankDetailStatCard.vue';
    import MyRankHistoryChart from '@/userSkills/myRank/MyRankHistoryChart.vue';
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
            MyRankHistoryChart,
            LevelsBreakdownChart,
            MyRankEncouragementCard,
        },
        props: {
            subject: String,
        },
        data() {
            return {
                loading: true,
                rankingDistribution: null,
            };
        },
        mounted() {
            this.getData();
        },
        methods: {
            getData() {
                this.loading = true;
                const subjectId = this.subject ? this.subject.subjectId : null;
                UserSkillsService.getUserSkillsRankingDistribution(subjectId)
                    .then((response) => {
                        this.rankingDistribution = response;
                        this.loading = false;
                    });
            },
        },
        computed: {
            numUsersBehindMe() {
                return this.rankingDistribution.totalUsers - this.rankingDistribution.myPosition;
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
