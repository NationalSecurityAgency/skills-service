package skills.utils

import groovy.transform.CompileStatic
import org.springframework.beans.BeanUtils

@CompileStatic
class Props {
    static void copy(Object source, Object target)  {
        // ignore groovy artifacts
        BeanUtils.copyProperties(source, target, "class", "metaClass")
    }

    static void copy(Object source, Object target, String... ignoreProperties)  {
        List<String> ignore = ["class", "metaClass"]
        ignore.addAll(ignoreProperties)

        // ignore groovy artifacts
        BeanUtils.copyProperties(source, target, ignore.toArray(new String[0]))
    }
}
