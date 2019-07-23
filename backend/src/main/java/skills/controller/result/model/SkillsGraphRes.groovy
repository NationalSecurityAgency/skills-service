package skills.controller.result.model

class SkillsGraphRes {
    static class Edge {
        Integer fromId
        Integer toId
    }

    List<SkillDefRes> nodes
    List<Edge> edges
}
