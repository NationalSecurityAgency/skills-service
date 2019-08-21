package skills.controller.result.model

class SkillsGraphRes {
    static class Edge {
        Integer fromId
        Integer toId
    }

    List<SkillDefGraphRes> nodes
    List<Edge> edges
}
