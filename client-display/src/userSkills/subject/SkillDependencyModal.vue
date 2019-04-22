<template>
    <transition name="node-transition" enter-active-class="animated zoomIn" leave-active-class="animated zoomOut">
        <div id="node-details-info" class="skill-details-container">
            <skill-dependency-card :skill="nodeDetailsView.skill" @close="close"/>
        </div>
    </transition>
</template>

<script>
    import SkillDependencyCard from '@/userSkills/subject/SkillDependencyCard.vue';

    export default {
        components: {
            SkillDependencyCard,
        },
        name: 'SkillDependencyModal',
        props: {
            nodeDetailsView: {
                type: Object,
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
    };
</script>

<style scoped>
    .skill-details-container {
        position: absolute;
        z-index: 100;
        max-width: 30rem;
    }

    .skill-details-container .skill-description {
        margin-top: 1rem;
        height: 12rem;
        overflow: auto;
    }

    .skill-description::-webkit-scrollbar-track {
        -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
        background-color: #F5F5F5;
        border-radius: 5px;
    }


    .skill-description::-webkit-scrollbar {
        width: 5px;
        background-color: #F5F5F5;
        border-radius: 5px;
    }

    .skill-description::-webkit-scrollbar-thumb {
        background-color: #585858;
        border-radius: 5px;
    }
</style>
