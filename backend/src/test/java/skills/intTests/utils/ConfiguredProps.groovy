package skills.intTests.utils

import groovy.util.logging.Slf4j

@Slf4j
class ConfiguredProps {

    Properties defaultProps = new Properties()
    // user props will override default props
    Properties userProps

    static ConfiguredProps configuredProps
    static synchronized ConfiguredProps get(){
        if (!configuredProps){
            configuredProps = new ConfiguredProps()
        }
        return configuredProps
    }

    private ConfiguredProps() {
        String propsFileLoc = System.getProperty("props.file")
        if (propsFileLoc){
            File userPropsFile = new File(propsFileLoc)
            if (!userPropsFile.exists()) {
                throw  new IllegalArgumentException("Provided user props file [${userPropsFile.absolutePath}] does not exist")
            }
            log.info("Loading user provided properties file [${userPropsFile.absolutePath}]")
            userProps = new Properties()
            userProps.load(userPropsFile.newReader())
        }

        this.getClass().getResource( '/test.properties' ).withInputStream {
            defaultProps.load(it)
        }
    }

    def getProp(String key){
        if (userProps && userProps."${key}") {
            return userProps."${key}"
        }

        return defaultProps."${key}"
    }

}
