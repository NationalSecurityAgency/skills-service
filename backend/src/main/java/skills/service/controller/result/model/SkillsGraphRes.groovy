package skills.service.controller.result.model

import skills.service.controller.request.model.SkillDefRes

class SkillsGraphRes {
    static class Edge {
        Integer fromId
        Integer toId
    }

    List<SkillDefRes> nodes
    List<Edge> edges
}
