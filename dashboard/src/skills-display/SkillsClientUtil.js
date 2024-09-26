import SkillsClientPath from '@/router/SkillsClientPath.js'

export default {

  isSkillsClientPath(){
    const path = window?.location?.pathname
    return path?.startsWith(SkillsClientPath.RootUrl) || path?.startsWith('/test-skills-client/')
  }
}