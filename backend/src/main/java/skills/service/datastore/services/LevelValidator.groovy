package skills.service.datastore.services

import skills.service.controller.exceptions.SkillException
import skills.service.controller.request.model.EditLevelRequest
import skills.service.controller.request.model.NextLevelRequest
import skills.storage.model.LevelDef

class LevelValidator {

    static void validateEditRequest(EditLevelRequest request, boolean asPoints){
        if(asPoints){
            //if it's the last level, points to could be null
            if(request.pointsTo != null && request.pointsFrom >= request.pointsTo){
                throw new SkillException("points from must be less than points to")
            }
        }else{
            if(request.percent <= 0){
                throw new SkillException("percent must be greater than zero")
            }
        }

        if(request.name?.length() > 50){
            throw new SkillException("name must be 50 characters or less")
        }

        if(request.level < 0){
            throw new SkillException("level must be greater than or equal to zero")
        }
    }

    static void validateNextLevelRequest(NextLevelRequest nlr, boolean asPoints){
        if(asPoints){
            //if it's the last level, points to could be null
            if(nlr.points > 0){
                throw new SkillException("points must be greater than zero")
            }
        }else{
            if(nlr.percent <= 0){
                throw new SkillException("percent must be greater than zero")
            }
        }

        if(nlr.name?.length() > 50){
            throw new SkillException("name must be 50 characters or less")
        }
    }

    static void validateLevelsBefore(LevelDef before, EditLevelRequest toValidate, boolean asPoints){
        if(before == null){
            return
        }

        boolean valid = true
        if(asPoints){
            if(before.pointsTo > toValidate.pointsFrom){
                valid = false
            }
        }else{
            if(before.percent > toValidate.percent){
                valid = false
            }
        }

        if(!valid){
            throw new SkillException("Edited Level's ${asPoints ? 'points from' : 'percent' } overlaps with previous level")
        }
    }

    static boolean validateLevelsAfter(LevelDef after, EditLevelRequest toValidate, boolean asPoints) {
        if (after == null) {
            return
        }

        boolean valid = true
        if(asPoints){
            if(toValidate.pointsTo > after.pointsFrom){
                valid = false
            }
        }else{
            if(toValidate.percent > after.percent){
                valid = false
            }
        }

        if(!valid){
            throw new SkillException("Edited Level's ${asPoints ? 'points to' : 'percent' } overlaps with next level")
        }
    }
}
