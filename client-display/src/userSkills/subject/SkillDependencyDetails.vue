<template>
    <transition name="node-transition" enter-active-class="animated zoomIn" leave-active-class="animated zoomOut">
        <div id="node-details-info" class="skill-details-container">

            <div class="panel panel-info">
                <div class="panel-heading">
                    <modal-header
                            slot="header"
                            class="text-left"
                            icon-class="fa fa-bar-chart"
                            :title="title"
                            @cancel="close"/>

                </div>
                <div class="panel-body text-left">
                    <div class="row">
                        <div class="col-md-9 text-left">
                            {{ progress.currentPoints }} / {{ progress.totalPoints }} Points
                        </div>
                        <div class="col-md-3 text-right">
                            <span class="text-muted">{{ progress.percentComplete }}%</span>
                        </div>
                    </div>
                    <div>
                        <progress-bar bar-color="lightgreen" :val="progress.percentComplete"></progress-bar>
                    </div>
                    <div>
                        <p class="skill-description">{{ nodeDetailsView.skill.description.description }}</p>
                    </div>
                </div>
                <div class="panel-footer" style="min-height: 5rem;">
                    <span v-show="nodeDetailsView.skill.description.href" class="pull-left">
                        <span>Need help?</span>
                        <a :href="nodeDetailsView.skill.description.href" target="_blank">
                            Click here!
                        </a>
                    </span>
                    <button class="btn btn-info pull-right" v-on:click="close">OK</button>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
    import ModalHeader from '@/common/modal/ModalHeader.vue';
    import VerticalProgress from '@/common/progress/VerticalProgress.vue';

    import ProgressBar from 'vue-simple-progress';

    export default {
        components: {
            VerticalProgress,
            ModalHeader,
            ProgressBar,
        },
        name: 'SkillDependencyDetails',
        props: {
            nodeDetailsView: {
                type: Object,
                required: true,
            },
        },
        mounted() {
            const detailsDiv = document.getElementById('node-details-info');
            const width = detailsDiv.offsetWidth;
            const height = detailsDiv.offsetHeight;
            let xPosition = this.nodeDetailsView.pointer.x - (width / 2);
            let yPosition = this.nodeDetailsView.pointer.y - (height / 2);

            yPosition = yPosition < 0 ? 0 : yPosition;
            xPosition = xPosition < 0 ? 0 : xPosition;

            detailsDiv.style.left = `${xPosition}px`;
            detailsDiv.style.top = `${yPosition}px`;
        },
        methods: {
            close() {
                this.$emit('close');
            },
        },
        computed: {
            title() {
                return this.nodeDetailsView.skill.skill;
            },
            progress() {
                return {
                    currentPoints: this.nodeDetailsView.skill.points,
                    totalPoints: this.nodeDetailsView.skill.totalPoints,
                    percentComplete: (this.nodeDetailsView.skill.points / this.nodeDetailsView.skill.totalPoints) * 100,
                };
            },
        },
    };
</script>

<style scoped>
    .skill-details-container {
        position: absolute;
        z-index: 100;
        max-width: 40rem;
    }

    .skill-details-container .skill-description {
        margin-top: 1rem;
        height: 15rem;
        overflow: auto;
    }

    .skill-description::-webkit-scrollbar-track
    {
        -webkit-box-shadow: inset 0 0 6px rgba(0,0,0,0.3);
        background-color: #F5F5F5;
        border-radius: 5px;
    }


    .skill-description::-webkit-scrollbar
    {
        width: 5px;
        background-color: #F5F5F5;
        border-radius: 5px;
    }

    .skill-description::-webkit-scrollbar-thumb
    {
        background-color: #585858;
        border-radius: 5px;
    }
</style>
