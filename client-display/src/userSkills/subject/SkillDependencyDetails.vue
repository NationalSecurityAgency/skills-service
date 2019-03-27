<template>
    <transition name="node-transition" enter-active-class="animated zoomIn" leave-active-class="animated zoomOut">
        <div id="node-details-info" class="skill-details-container">

            <div class="panel panel-default">
                <div class="panel-heading">
                    <modal-header
                            slot="header"
                            class="text-left"
                            icon-class="fa fa-bar-chart"
                            :title="nodeDetailsView.skill.skill"
                            @cancel="close"/>

                </div>
                <div class="panel-body text-left">
                    <div>
                        <vertical-progress
                                v-if="progress.total === 100"
                                total-progress-bar-color="#59ad52"
                                before-today-bar-color="#59ad52"
                                :total-progress="progress.total"
                                :total-progress-before-today="progress.totalBeforeToday"
                        />
                        <vertical-progress
                                v-if="nodeDetailsView.skill.points !== nodeDetailsView.skill.totalPoints && progress.total !== 100"
                                :total-progress="progress.total"
                                :total-progress-before-today="progress.totalBeforeToday"
                                :is-locked="locked"
                        />
                    </div>
                    <div>
                        {{ nodeDetailsView.skill.description.description }}
                    </div>
                </div>
                <div class="panel-footer">
                    <button class="btn btn-default" v-on:click="close">OK</button>
                </div>
            </div>
        </div>
    </transition>
</template>

<script>
    import ModalHeader from '@/common/modal/ModalHeader.vue';
    import VerticalProgress from '@/common/progress/VerticalProgress.vue';

    export default {
        components: {
            VerticalProgress,
            ModalHeader,
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
            detailsDiv.style.left = `${this.nodeDetailsView.pointer.x - (width / 2)}px`;
            detailsDiv.style.top = `${this.nodeDetailsView.pointer.y - (height / 2)}px`;
        },
        methods: {
            close() {
                this.$emit('close');
            },
        },
        computed: {
            progress() {
                return {
                    total: (this.nodeDetailsView.skill.points / this.nodeDetailsView.skill.totalPoints) * 100,
                    totalBeforeToday: ((this.nodeDetailsView.skill.points - this.nodeDetailsView.skill.todaysPoints) / this.nodeDetailsView.skill.totalPoints) * 100,
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
</style>
