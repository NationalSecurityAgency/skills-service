<script>
    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    export default {
        name: 'SkillDisplayDataLoadingMixin',
        data() {
            return {
                loading: {
                    userSkills: true,
                    pointsHistory: true,
                    userSkillsRanking: true,
                },
                displayData: {
                    userSkills: null,
                    pointsHistory: null,
                    userSkillsRanking: null,
                },
            };
        },
        methods: {
            loadUserSkills() {
                UserSkillsService.getUserSkills()
                    .then((response) => {
                        this.displayData.userSkills = response;
                        this.loading.userSkills = false;
                    });
            },
            loadSubject() {
                UserSkillsService.getSubjectSummary(this.$route.params.subjectId)
                    .then((result) => {
                        this.displayData.userSkills = result;
                        this.loading.userSkills = false;
                    });
            },
            loadUserSkillsRanking() {
                UserSkillsService.getUserSkillsRanking(this.$route.params.subjectId)
                    .then((response) => {
                        this.displayData.userSkillsRanking = response;
                        this.loading.userSkillsRanking = false;
                    });
            },

            loadPointsHistory() {
                UserSkillsService.getPointsHistory(this.$route.params.subjectId)
                    .then((result) => {
                        this.displayData.pointsHistory = result;
                        this.loading.pointsHistory = false;
                    });
            },
            resetLoading() {
                this.loading.userSkills = true;
                this.loading.pointsHistory = true;
                this.loading.userSkillsRanking = true;
            },
        },
        computed: {
            isLoaded() {
                return !this.loading.userSkills && !this.loading.pointsHistory && !this.loading.userSkillsRanking;
            },
        },
    };
</script>

<style scoped>

</style>
