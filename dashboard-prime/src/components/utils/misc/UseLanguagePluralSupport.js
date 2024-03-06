export const useLanguagePluralSupport = () => {
  const plural = (arr) => {
    return arr && arr.length > 1 ? 's' : ''
  }

  const pluralWithHave = (arr) => {
    return arr && arr.length > 1 ? 's have' : ' has'
  }

  return {
    plural,
    pluralWithHave
  }
}