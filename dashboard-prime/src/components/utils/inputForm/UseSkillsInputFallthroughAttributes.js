import { computed, useAttrs } from 'vue'

export const useSkillsInputFallthroughAttributes = () => {
  const attrs = useAttrs()

  const filterAttrs = (attrsToFilter, filterKeys) => {
    return Object.fromEntries(
      Object.entries(attrsToFilter).filter(([key]) => !filterKeys.includes(key))
    )
  }
  const inputAttrs = computed(() => {
    return filterAttrs(attrs, ['class', 'data-cy'])
  })
  const rootAttrs = computed(() => {
    return filterAttrs(attrs, ['data-cy'])
  })

  return {
    inputAttrs,
    rootAttrs
  }
}