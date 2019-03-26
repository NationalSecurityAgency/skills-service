<template>
    <section>
        <modal @dismiss="handleClose">
            <modal-header
                    slot="header"
                    class="text-left"
                    icon-class="fa fa-bar-chart"
                    title="Dependency Graph"
                    @cancel="handleClose"/>

            <div>
                <graph-legend class="graph-legend" :items="[
                    {label: 'This Skill', color: 'lightblue'},
                    {label: 'Dependencies', color: 'lightgray'},
                    {label: 'Achieved Dependencies', color: 'lightgreen'}
                    ]">
                </graph-legend>
                <div id="dependent-skills-network" style="height: 500px"></div>
            </div>

            <div slot="footer">
                <button class="btn btn-primary pull-right" type="button" @click="handleClose">
                    OK
                </button>
            </div>
        </modal>
    </section>
</template>

<script>
    import vis from 'vis';
    import 'vis/dist/vis.css';
    import Modal from '@/common/modal/Modal.vue';
    import ModalHeader from '@/common/modal/ModalHeader.vue';
    import UserSkillsService from '@/userSkills/service/UserSkillsService';
    import GraphLegend from '@/userSkills/subject/GraphLegend.vue';

    export default {
        name: 'SkillDependencyGraph',
        components: { GraphLegend, Modal, ModalHeader },
        props: {
            skill: {
                type: Object,
                required: true,
            },
        },
        data() {
            return {
                network: null,
                displayOptions: {
                    layout: {
                        randomSeed: 419465,
                        hierarchical: {
                            enabled: true,
                            sortMethod: 'directed',
                            nodeSpacing: 350,
                            // treeSpacing: 1000,
                            // blockShifting: false,
                            // edgeMinimization: false,
                            // parentCentralization: false,
                            // levelSeparation: 1000,
                            // direction: 'UP',
                        },
                    },
                    interaction: {
                        selectConnectedEdges: false,
                        navigationButtons: true,
                    },
                    physics: {
                        enabled: false,
                    },
                    nodes: {
                        font: {
                            size: 18,
                        },
                        color: {
                            border: '#868686',
                            background: '#e4e4e4',
                        },
                        mass: 20,
                    },
                },
            };
        },
        mounted() {
            UserSkillsService.getSkillDependencies(this.skill.skillId)
                .then((res) => {
                    this.createGraph(res.dependencies);
            });
        },
        methods: {
            handleClose() {
                this.$emit('ok');
            },
            createGraph(dependencies) {
                const data = this.buildData(dependencies);
                const container = document.getElementById('dependent-skills-network');
                this.network = new vis.Network(container, data, this.displayOptions);
            },
            buildData(dependencies) {
                const nodes = new vis.DataSet();
                const edges = new vis.DataSet();
                const createdSkillIds = [];

                // color: { border: '#3273dc', background: 'lightblue' },
                // nodes.add(this.buildNode(this.skill, { color: { border: 'green', background: 'lightgreen' } }));
                dependencies.forEach((item) => {
                    const isThisSkill = this.skill.projectId === item.skill.projectId && this.skill.skillId === item.skill.skillId;
                    const extraParentProps = isThisSkill ? { color: { border: '#3273dc', background: 'lightblue' } } : {};
                    this.buildNode(item.skill, createdSkillIds, nodes, extraParentProps);

                    const extraChildProps = item.achieved ? { color: { border: 'green', background: 'lightgreen' } } : {};
                    this.buildNode(item.dependsOn, createdSkillIds, nodes, extraChildProps);
                    edges.add({
                        from: this.getNodeId(item.skill),
                        to: this.getNodeId(item.dependsOn),
                        arrows: 'to',
                    });
                });

                const data = { nodes, edges };
                return data;
            },
            buildNode(skill, createdSkillIds, nodes, extraProps = {}) {
                if (!createdSkillIds.includes(skill.skillId)) {
                    createdSkillIds.push(skill.skillId);

                    const isCrossProj = skill.projectId !== this.skill.projectId;
                    const node = {
                        id: this.getNodeId(skill),
                        label: isCrossProj ? `${skill.projectName} : ${skill.skillName}` : skill.skillName,
                        margin: 10,
                        shape: 'box',
                        chosen: false,
                    };
                    const res = Object.assign(node, extraProps);
                    nodes.add(res);
                }
            },
            getNodeId(skill) {
                return `${skill.projectName}_${skill.skillId}`;
            },
        },
    };
</script>

<style scoped>
    .graph-legend {
        position: absolute;
        z-index: 10;
    }
</style>


<style>
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-up,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-down,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-left,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-right,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomIn,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomOut,
    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button.vis-zoomExtends {
        background-image: none !important;
    }

    #dependent-skills-network div.vis-network div.vis-navigation div.vis-button:hover {
        box-shadow: none !important;
    }

    #dependent-skills-network .vis-button:after {
        font-size: 2.7em;
        color: gray;
        font-family: "Font Awesome 5 Free";
    }

    #dependent-skills-network .vis-button:hover:after {
        font-size: 2em;
        color: #3273dc;
    }

    #dependent-skills-network .vis-button.vis-up:after {
        content: '\f35b';
    }

    #dependent-skills-network .vis-button.vis-down:after {
        content: '\f358';
    }

    #dependent-skills-network .vis-button.vis-left:after {
        content: '\f359';
    }

    #dependent-skills-network .vis-button.vis-right:after {
        content: '\f35a';
    }

    #dependent-skills-network .vis-button.vis-zoomIn:after {
        content: '\f0fe';
    }

    #dependent-skills-network .vis-button.vis-zoomOut:after {
        content: '\f146';
    }

    #dependent-skills-network .vis-button.vis-zoomExtends:after {
        content: "\f78c";
        font-weight: 900;
        font-size: 30px;
    }

</style>
