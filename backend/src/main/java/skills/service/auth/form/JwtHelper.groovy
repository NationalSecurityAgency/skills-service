package skills.service.auth.form

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import skills.service.auth.UserAuthService
import skills.service.auth.UserInfo

import javax.annotation.PostConstruct
import javax.crypto.KeyGenerator
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Configuration
@ConfigurationProperties(prefix = 'skills.authorization.form')
@Slf4j
class JwtHelper {

    static final String ROLES_CLAIM = 'roles'
    static final String TOKEN_PREFIX = 'Bearer '
    static final String TOKEN_HEADER = 'Authorization'
    static final String SIGNATURE_ALGORITHM = 'HmacSHA512'
    static final String TOKEN_EXPIRATION_HEADER = 'TokenExpirationTimestamp'

    static final Long DEFAULT_EXPIRATION_TIME = 864_000_000 // 10 days

    String base64EncodedSecret
    Long expirationTime = DEFAULT_EXPIRATION_TIME
    byte[] secretKey

    @Autowired
    UserAuthService userAuthService

    @PostConstruct
    private byte [] loadSecretkey() {
        if (base64EncodedSecret) {
            this.secretKey = Base64.decoder.decode(base64EncodedSecret)
        } else {
            log.info("Generating random JWT secret key...")
            this.secretKey = KeyGenerator.getInstance(SIGNATURE_ALGORITHM).generateKey().getEncoded()
        }
    }

    UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication
        String token = request.getHeader(TOKEN_HEADER)
        if (token != null) {
            DecodedJWT jwt
            try {
                jwt = JWT.require(Algorithm.HMAC512(secretKey))
                        .build()
                        .verify(token.replace(TOKEN_PREFIX, ''))
                String subject = jwt.getSubject()
                if (subject != null) {
                    UserInfo principal = userAuthService.loadByUserId(subject)
                    authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                }
            } catch(JWTVerificationException e) {
                log.warn('Unable to verify JWT token')
//                throw new BadCredentialsException(e.message, e)
            }
        }
        return authentication
    }

    void addToken(HttpServletResponse response, UserDetails userDetails) {
        Date expiration = new Date(System.currentTimeMillis() + expirationTime)
        String token = JWT.create()
                .withSubject(userDetails.getUsername())
                .withExpiresAt(expiration)
                .withClaim(ROLES_CLAIM, userDetails.getAuthorities()?.collect {it.getAuthority()}.join(','))
                .sign(Algorithm.HMAC512(secretKey))
        response.addHeader(TOKEN_HEADER, TOKEN_PREFIX + token)
        response.addHeader(TOKEN_EXPIRATION_HEADER, "${expiration.time}")
    }

}
