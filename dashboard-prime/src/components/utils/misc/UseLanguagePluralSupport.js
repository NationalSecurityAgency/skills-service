export const useLanguagePluralSupport = () => {
  const plural = (param) => {
    if (param instanceof Array) {
      return param && param.length > 1 ? 's' : ''
    }
    if (param instanceof Number) {
      return  param > 1 ? 's' : ''
    }

    return ''
  }

  const areOrIs = (numItems) => {
    return (numItems > 1) ? 'are' : 'is';
  }
  const sOrNone = (numItems) => {
    return (numItems > 1) ? 's' : '';
  }

  const pluralWithHave = (arr) => {
    return arr && arr.length > 1 ? 's have' : ' has'
  }

  return {
    plural,
    pluralWithHave,
    areOrIs,
    sOrNone
  }
}