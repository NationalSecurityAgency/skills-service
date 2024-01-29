import { nextTick } from 'vue'
import { useAnnouncer } from '@vue-a11y/announcer'

export const useSkillsAnnouncer = () => {

  const announcer = useAnnouncer()
  const polite = (msg) => {
    nextTick(() => announcer.polite(msg))
  }

  const assertive = (msg) => {
    nextTick(() => announcer.assertive(msg))
  }

  return {
    polite,
    assertive
  }
}