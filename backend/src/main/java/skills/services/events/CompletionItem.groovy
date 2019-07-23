package skills.services.events

import groovy.transform.Canonical

@Canonical
class CompletionItem {
    static enum CompletionItemType {
        Overall, Subject, Skill, Badge
    };
    CompletionItemType type
    Integer level // optional
    String id
    String name
    List<RecommendationItem> recommendations = []
}
