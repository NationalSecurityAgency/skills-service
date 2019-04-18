<template>
    <div>
        <ribbon v-if="badge" :color="ribbonColor">
            {{ badge.badge }}
        </ribbon>

        <div class="card">
            <div class="card-body">
                <badge-details-overview :badge="badge"></badge-details-overview>
            </div>
        </div>

        <skills-progress-list :subject="badge" :show-descriptions="showDescriptions"/>

        <div class="pull-left">
      <span>
        Need help?
        <a
                :href="helpTipHref" style="padding-right: 10px"
                target="_blank">Click here!</a>
      </span>
            <div class="description-toggle-container">
        <span>
          <span class="text-muted">User skills descriptions:&nbsp;</span>
        </span>
                <toggle-button
                        v-model="showDescriptions"
                        :labels="{ checked: 'On', unchecked: 'Off' }"
                        @change="toggleDescriptions">
                </toggle-button>
            </div>
        </div>
    </div>
</template>

<script>
    import Ribbon from '@/common/ribbon/Ribbon.vue';
    import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview.vue';
    import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';
    import Popper from 'vue-popperjs';
    import marked from 'marked';

    import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    import 'vue-popperjs/dist/vue-popper.css';

    export default {
        components: {
            Ribbon,
            SkillsProgressList,
            ToggleButton,
            Popper,
            BadgeDetailsOverview,
        },
        data() {
            return {
                badge: null,
                ribbonColor: {
                    default: 'gold',
                },
                initialized: false,
                showDescriptions: false,
            };
        },
        computed: {
            gemExpirationDate() {
                let dateString = '';
                if (this.badge.gem) {
                    // Parse date manually. avoid large moment.js import for such a small thing..
                    dateString = this.badge.endDate.replace(/T.*/, '');
                }
                return dateString;
            },

            helpTipHref() {
                return '';
            },
        },
        watch: {
            $route: 'fetchData',
        },
        mounted() {
            this.fetchData();
        },
        methods: {
            fetchData() {
                this.ribbonColor = this.$route.query.ribbonColor;
                UserSkillsService.getBadgeSkills(this.$route.params.badgeId)
                    .then((badgeSummary) => {
                        this.badge = badgeSummary;
                    });
            },

            parseMarkdown(text) {
                return marked(text);
            },

            toggleDescriptions(event) {
                this.showDescriptions = event.value;
            },

            handleClose() {
                this.$emit('ok');
            },
        },
    };
</script>

<style scoped>
    .badge-detail-container {
        max-width: 875px;
        margin: 0 auto;
    }

    .badge-body {
        background-color: #fcfcfc;
    }

    .badge-description-icon {
        color: gold;
        font-size: 80px;
        display: inline-block;
    }

    .description-toggle-container {
        display: inline-block;
        padding-left: 25px;
    }

    .user-skill-subject-description {
        text-align: center;
        font-style: italic;
        padding: 10px;
    }

    .user-skill-subject-description p {
        max-width: 375px;
    }

    .gem-indicator {
        color: #FF7070;
        font-size: 25px;
    }
</style>
