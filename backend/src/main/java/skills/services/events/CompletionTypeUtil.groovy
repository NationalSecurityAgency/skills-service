package skills.services.events

import groovy.transform.CompileStatic
import skills.storage.model.SkillDef

@CompileStatic
class CompletionTypeUtil {

    static CompletionItem.CompletionItemType getCompletionType(SkillDef.ContainerType type){
        switch (type) {
            case SkillDef.ContainerType.Subject:
                return CompletionItem.CompletionItemType.Subject
            case SkillDef.ContainerType.Badge:
                return CompletionItem.CompletionItemType.Badge
            case SkillDef.ContainerType.GlobalBadge:
                return CompletionItem.CompletionItemType.GlobalBadge
            default:
                throw new IllegalStateException("this method doesn't support type $type")
        }
    }
}
