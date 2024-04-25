export const useLanguagePluralSupport = () => {
  const plural = (param) => {
    let res = ''
    if (param instanceof Array) {
      res = param && param.length > 1 ? 's' : ''
    }
    if (typeof param === 'number') {
      res=  param !== 1 ? 's' : ''
    }
    return res
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