import TruncateFilter from '../../../filters/TruncateFilter';

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
    let res = isCrossProject ? `${this.truncate(skillItem.projectId, 10)} : ${skillItem.name} ` : skillItem.name;
    res = this.truncate(res);
    return res;
  },
  truncate(strValue, truncateTo = 35) {
    return TruncateFilter(strValue, truncateTo);
  },

};
