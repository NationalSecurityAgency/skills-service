
export const useCheckIfAnswerChangedForValidation = () => {
  const cache = new Map()
  const hasValueChanged = (newValue, testContext) => {
    const quizAnswers = testContext?.parent.quizAnswers
    if (quizAnswers && quizAnswers.length === 1 && quizAnswers[0].id) {
      const answerId = quizAnswers[0].id
      const cachedValue = cache.get(answerId)
      if (cachedValue === newValue) {
        return false
      }
      cache.set(answerId, newValue)
    }
    return true
  }
  const reset = () => {
    cache.clear()
  }
  return {
    hasValueChanged,
    reset
  }
}