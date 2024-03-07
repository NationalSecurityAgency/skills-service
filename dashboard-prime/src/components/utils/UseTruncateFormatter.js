export const useTruncateFormatter = () => {
  const truncate = (strValue, truncateTo = 30) => {
    const regexStr = `^.{0,${truncateTo}}[\\S]*`;
    const regex1 = new RegExp(regexStr, 'gi');

    let re = strValue.match(regex1);
    const l = re[0].length;
    re = re[0].replace(/\s$/, '');
    if (l < strValue.length) {
      re = `${re}...`;
    }

    const regexStr2 = `(.{${truncateTo}})..+`;
    const regex2 = new RegExp(regexStr2, 'gi');
    re = re.replace(regex2, '$1...');
    return re;
  }

  return {
    truncate,
  }
}