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
package skills.utils

import groovy.transform.CompileStatic
import org.springframework.beans.BeanUtils

@CompileStatic
class Props {
    static Object copy(Object source, Object target)  {
        // ignore groovy artifacts
        BeanUtils.copyProperties(source, target, "class", "metaClass")
        return target
    }

    static Object copy(Object source, Object target, String... ignoreProperties)  {
        List<String> ignore = ["class", "metaClass"]
        ignore.addAll(ignoreProperties)

        String[] ignoreProps = ignore.toArray(new String[0])
        // ignore groovy artifacts
        BeanUtils.copyProperties(source, target, ignoreProps)

        return target
    }
}
