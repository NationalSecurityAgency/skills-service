export default {
  getTitle(skillItem, isCrossProject) {
    let crossProjInfo = '';
    if (isCrossProject) {
      crossProjInfo = `<span style="border-bottom: 1px dotted black; font-weight: bold;"><i class="fas fa-handshake"></i> Cross Project Dependency</span><br/>
                           <span>Project ID: ${skillItem.projectId}</span><br/>`;
    }
    return `${crossProjInfo}<span style="font-style: italic; color: #444444">Name:</span> ${skillItem.name}<br/>
                <span style="font-style: italic; color: #444444">ID:</span> ${skillItem.skillId}<br/>
                <span style="font-style: italic; color: #444444">Point Increment:</span> ${skillItem.pointIncrement}<br/>
                <span style="font-style: italic; color: #444444">Total Points:</span> ${skillItem.totalPoints}`;
  },
  getLabel(skillItem, isCrossProject) {
    let res = isCrossProject ? `${skillItem.projectId} : ${skillItem.name} ` : skillItem.name;
    res = this.truncate(res);
    return res;
  },
  truncate(strValue) {
    let re = strValue.match(/^.{0,30}[\S]*/);
    const l = re[0].length;
    re = re[0].replace(/\s$/, '');
    if (l < strValue.length) {
      re = `${re}...`;
    }

    re = re.replace(/(.{35})..+/, '$1...');
    return re;
  },

};
