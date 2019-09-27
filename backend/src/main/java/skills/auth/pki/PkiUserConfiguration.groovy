package skills.auth.pki


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import skills.auth.SecurityMode

@Conditional(SecurityMode.PkiAuth)
@Configuration
class PkiUserConfiguration {
    @Bean
    @Conditional(SecurityMode.PkiAuth)
    PkiUserLookup pkiUserLookup() {
        new PkiUserLookup()
    }
}
