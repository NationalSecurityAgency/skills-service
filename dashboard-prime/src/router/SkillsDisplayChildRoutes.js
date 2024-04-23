import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import SkillsDisplayErrorPage from '@/skills-display/components/errors/SkillsDisplayErrorPage.vue'
import MyRankDetailsPage from '@/skills-display/components/rank/MyRankDetailsPage.vue'
import SubjectDetailsPage from '@/skills-display/components/subjects/SubjectPage.vue'
import SkillPage from '@/skills-display/components/skill/SkillPage.vue'
import BadgesDetailsPage from '@/skills-display/components/badges/BadgesDetailsPage.vue'
import QuizPage from '@/skills-display/components/skills/QuizPage.vue';

const createSkillsDisplayChildRoutes = (appendToName) => {

  const projectPlaceholder = '##PROJECT##';
  const projectPlaceholderRegex = new RegExp(projectPlaceholder, 'g');
  const subjectPlaceholder = '##SUBJECT##';
  const subjectPlaceholderRegex = new RegExp(subjectPlaceholder, 'g');
  const groupPlaceholder = '##GROUP##';
  const groupPlaceholderRegex = new RegExp(groupPlaceholder, 'g');
  const skillPlaceholder = '##SKILL##';
  const skillPlaceholderRegex = new RegExp(skillPlaceholder, 'g');


  return [{
    name: `SkillsDisplay${appendToName}`,
    path: '',
    component: SkillsDisplay,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    }
  }, {
    name: `MyRankDetailsPage${appendToName}`,
    path: 'rank',
    component: MyRankDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'My Rank'
      }
    }
  }, {
    name: `BadgesDetailsPage${appendToName}`,
    path: 'badges',
    component: BadgesDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Badges'
      }
    }
  }, {
    name: `SubjectDetailsPage${appendToName}`,
    path: 'subjects/:subjectId',
    component: SubjectDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Subject'
      }
    }
  }, {
    path: 'subjects/:subjectId/skills/:skillId',
    component: SkillPage,
    name: `skillDetails${appendToName}`,
    meta: {
      title: `${skillPlaceholder} Details`,
    },
  }, {
    name: `quizPage${appendToName}`,
    path: 'subjects/:subjectId/skills/:skillId/quizzes/:quizId',
    component: QuizPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Quiz or Survey Run'
      }
    },
  }, {
    name: `SkillsDisplayErrorPage${appendToName}`,
    path: 'error',
    component: SkillsDisplayErrorPage,
    meta: {
      requiresAuth: false,
      nonAdmin: true,
      announcer: {
        message: 'Error Page'
      }
    }
  }]
}

export default createSkillsDisplayChildRoutes
