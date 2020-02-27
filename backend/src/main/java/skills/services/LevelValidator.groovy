/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services


import skills.storage.model.LevelDef

class LevelValidator {

    static void validateEditRequest(skills.controller.request.model.EditLevelRequest request, boolean asPoints){
        if(asPoints){
            //if it's the last level, points to could be null
            if(request.pointsTo != null && request.pointsFrom >= request.pointsTo){
                throw new skills.controller.exceptions.SkillException("points from must be less than points to")
            }
        }else{
            if(request.percent <= 0){
                throw new skills.controller.exceptions.SkillException("percent must be greater than zero")
            }
        }

        if(request.name?.length() > 50){
            throw new skills.controller.exceptions.SkillException("name must be 50 characters or less")
        }

        if(request.level < 0){
            throw new skills.controller.exceptions.SkillException("level must be greater than or equal to zero")
        }
    }

    static void validateNextLevelRequest(skills.controller.request.model.NextLevelRequest nlr, boolean asPoints){
        if(asPoints){
            //if it's the last level, points to could be null
            if(nlr.points > 0){
                throw new skills.controller.exceptions.SkillException("points must be greater than zero")
            }
        }else{
            if(nlr.percent <= 0){
                throw new skills.controller.exceptions.SkillException("percent must be greater than zero")
            }
        }

        if(nlr.name?.length() > 50){
            throw new skills.controller.exceptions.SkillException("name must be 50 characters or less")
        }
    }

    static void validateLevelsBefore(LevelDef before, skills.controller.request.model.EditLevelRequest toValidate, boolean asPoints){
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
            throw new skills.controller.exceptions.SkillException("Edited Level's ${asPoints ? 'points from' : 'percent' } overlaps with previous level")
        }
    }

    static boolean validateLevelsAfter(LevelDef after, skills.controller.request.model.EditLevelRequest toValidate, boolean asPoints) {
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
            throw new skills.controller.exceptions.SkillException("Edited Level's ${asPoints ? 'points to' : 'percent' } overlaps with next level")
        }
    }
}
