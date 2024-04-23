import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'

export const useSkillsDisplaySubjectState = defineStore('skillDisplaySubjectState', () => {

  // const servicePath = '/api/projects'
  // const attributes = useSkillsDisplayAttributesState()

  const skillsDisplayService = useSkillsDisplayService()

  const loadingSubjectSummary = ref(true)
  const subjectSummary = ref({})

  // const getUserIdAndVersionParams = () => {
  //   // const params = this.getUserIdParams();
  //   // params.version = this.version;
  //   //
  //   // return params;
  //   return {}
  // }
  const loadSubjectSummary = (subjectId, includeSkills = true) =>{
    skillsDisplayService.loadSubjectSummary(subjectId, includeSkills)
      .then((res) => {
        subjectSummary.value = res
      }).finally(() => {
      loadingSubjectSummary.value = false
    })


    // const params = getUserIdAndVersionParams();
    // params.includeSkills = includeSkills;
    // return axios.get(`${attributes.serviceUrl}${servicePath}/${encodeURIComponent(attributes.projectId)}/subjects/${encodeURIComponent(subjectId)}/summary`, {
    //   params,
    // }).then((result) => {
    //   subjectSummary.value = addMetaToSummary(result.data)
    // }).finally(() => {
    //   loadingSubjectSummary.value = false
    // });
  }

  // const addMetaToSummary = (summary) => {
  //   const res = summary;
  //   res.skills = res.skills.map((item) => {
  //     const skillRes = addMeta(item);
  //     if (item.type === 'SkillsGroup' && item.children) {
  //       skillRes.children = skillRes.children.map((child) => addMeta(child));
  //       const numSkillsRequired = skillRes.numSkillsRequired === -1 ? skillRes.children.length : skillRes.numSkillsRequired;
  //       const numSkillsCompleted = skillRes.children.filter((childSkill) => childSkill.meta.complete).length;
  //       skillRes.meta.complete = numSkillsCompleted >= numSkillsRequired;
  //     }
  //
  //     return skillRes;
  //   });
  //   return res;
  // }
  //
  // const addMeta =(skill) => {
  //   const copy = { ...skill };
  //   copy.meta = {
  //     complete: skill.points >= skill.totalPoints,
  //     withoutProgress: skill.points === 0,
  //     inProgress: skill.points > 0 && skill.points < skill.totalPoints,
  //     pendingApproval: skill.selfReporting && skill.selfReporting.requestedOn && !skill.selfReporting.rejectedOn,
  //     belongsToBadge: skill.badges && skill.badges.length > 0,
  //     hasTag: skill.tags && skill.tags.length > 0,
  //     approval: skill.selfReporting && skill.selfReporting.type === 'Approval',
  //     honorSystem: skill.selfReporting && skill.selfReporting.type === 'HonorSystem',
  //     quiz: skill.selfReporting && skill.selfReporting.type === 'Quiz',
  //     survey: skill.selfReporting && skill.selfReporting.type === 'Survey',
  //     video: skill.selfReporting && skill.selfReporting.type === 'Video',
  //   };
  //   return copy;
  // }


  return {
    loadSubjectSummary,
    loadingSubjectSummary,
    subjectSummary,
  }

})