import { computed, useAttrs } from 'vue'

export const useSkillsInputFallthroughAttributes = () => {
  const attrs = useAttrs()

  const filterAttrs = (attrsToFilter, filterKeys) => {
    return Object.fromEntries(
      Object.entries(attrsToFilter).filter(([key]) => !filterKeys.includes(key))
    )
  }
  const inputAttrs = computed(() => {
    let newAttrs = filterAttrs(attrs, ['class', 'data-cy']);
    if(newAttrs['input-class']) {
      newAttrs.class = newAttrs['input-class'];
      delete newAttrs['input-class'];
    }
    return newAttrs;
  })
  const rootAttrs = computed(() => {
    return filterAttrs(attrs, ['data-cy', 'input-class'])
  })

  return {
    inputAttrs,
    rootAttrs
  }
}