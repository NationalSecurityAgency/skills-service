package skills.intTests.utils

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

        // ignore groovy artifacts
        BeanUtils.copyProperties(source, target, ignore.toArray(new String[0]))

        return target
    }
}
