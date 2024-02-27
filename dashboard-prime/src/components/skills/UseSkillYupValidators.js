import { useDebounceFn } from '@vueuse/core'
import SkillsService from '@/components/skills/SkillsService.js'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import { useRoute } from 'vue-router'

export const useSkillYupValidators = () => {

  const appConfig = useAppConfig()
  const route = useRoute()

  const checkSkillNameUnique = useDebounceFn((value, origValue, isEdit) => {
    if (!value || value.length === 0) {
      return true
    }
    if (isEdit && (origValue === value || origValue.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
      return true
    }
    return SkillsService.skillWithNameExists(route.params.projectId, value).then((remoteRes) => remoteRes)
  }, appConfig.formFieldDebounceInMs)

  const checkSkillIdUnique = useDebounceFn((value, origValue, isEdit) => {
    if (!value || value.length === 0 || (isEdit && origValue === value)) {
      return true
    }
    return SkillsService.skillWithIdExists(route.params.projectId, value)
      .then((remoteRes) => remoteRes)

  }, appConfig.formFieldDebounceInMs)

  return {
    checkSkillNameUnique,
    checkSkillIdUnique
  }
}