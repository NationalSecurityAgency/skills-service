export const useErrorChecker = () => {

  const checkExceptionErrorCode = (exception, errorCode) => {
    return exception.response &&
      exception.response.data &&
      exception.response.data.errorCode &&
      exception.response.data.errorCode === errorCode
  }
  const isLearningPathErrorCode = (exception) => {
    return checkExceptionErrorCode(exception, 'LearningPathViolation')
  }

  return {
    checkExceptionErrorCode,
    isLearningPathErrorCode
  }
}