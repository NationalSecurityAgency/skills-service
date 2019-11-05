package skills.services.admin

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.request.model.ActionPatchRequest
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo

@Service
@Slf4j
class DisplayOrderService {

    @Autowired
    SkillDefRepo skillDefRepo

    void resetDisplayOrder(List<SkillDef> skillDefs) {
        if(skillDefs) {
            List <SkillDef> copy = new ArrayList<>(skillDefs)
            List<SkillDef> toSave = []
            copy = copy.sort({ it.displayOrder })
            copy.eachWithIndex { SkillDef entry, int i ->
                if (entry.displayOrder != i) {
                    toSave.add(entry)
                    entry.displayOrder = i
                }
            }
            if (toSave) {
                skillDefRepo.saveAll(toSave)
            }
        }
    }

    @Transactional
    void updateDisplayOrder(String skillId, List<SkillDef> skills, ActionPatchRequest patchRequest) {
        SkillDef toUpdate = skills.find({ it.skillId == skillId })

        SkillDef switchWith

        switch (patchRequest.action) {
            case ActionPatchRequest.ActionType.DisplayOrderDown:
                skills = skills.sort({ it.displayOrder })
                switchWith = skills.find({ it.displayOrder > toUpdate.displayOrder })
                break;
            case ActionPatchRequest.ActionType.DisplayOrderUp:
                skills = skills.sort({ it.displayOrder }).reverse()
                switchWith = skills.find({ it.displayOrder < toUpdate.displayOrder })
                break;
            default:
                throw new IllegalArgumentException("Unknown action ${patchRequest.action}")
        }

        if (!switchWith) {
            assert switchWith, "Failed to find definition to switch with [${toUpdate?.skillId}] for action [$patchRequest.action]"
        }
        assert switchWith.skillId != toUpdate.skillId

        int switchWithDisplayOrderTmp = toUpdate.displayOrder

        toUpdate.displayOrder = switchWith.displayOrder
        switchWith.displayOrder = switchWithDisplayOrderTmp
        skillDefRepo.saveAll([toUpdate, switchWith])

        log.debug("Switched order of [{}] and [{}]", toUpdate.skillId, switchWith.skillId)
    }
}
