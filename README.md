![SkillTree](skilltree_logo.png)

# SkillTree Service, Dashboard and Client Display
SkillTree is a micro-learning gamification platform providing out-of-the-box UI visualizations, convenient client integration libraries, and a dashboard for managing the creation and administration of Gamified Training Profiles.  

To learn more about the SkillTree platform please visit our [Official Documentation](https://code.nsa.gov/skills-docs/). 
These pages provide in-depth guidance on installation, usage and contribution.    


# Workflows Status
[![Continuous Integration](https://github.com/NationalSecurityAgency/skills-service/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/NationalSecurityAgency/skills-service/actions/workflows/build-and-test.yml)


## Amazon Cognito Sign-In

In Password Auth Mode, configure a Cognito user pool app client for the authorization code grant.
Register `https://<skilltree-host>/login/oauth2/code/cognito` as an allowed callback URL.
Use `cognito` as the registration ID so SkillTree selects its Cognito user converter:

```properties
spring.security.oauth2.client.registration.cognito.client-id=${COGNITO_CLIENT_ID}
spring.security.oauth2.client.registration.cognito.client-secret=${COGNITO_CLIENT_SECRET}
spring.security.oauth2.client.registration.cognito.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.cognito.scope=openid,profile,email
spring.security.oauth2.client.registration.cognito.client-name=Amazon Cognito
spring.security.oauth2.client.provider.cognito.issuer-uri=https://cognito-idp.<region>.amazonaws.com/<user-pool-id>
spring.security.oauth2.client.provider.cognito.user-name-attribute=sub
```

Keep the client secret in deployment configuration, outside the repository. Enable read access to
`given_name`, `family_name` and `email` for the app client and populate those attributes for users.
SkillTree requires these fields and uses the immutable `sub` claim, suffixed with `-cognito`, as the
account ID. Email changes do not change that ID. Spring Security handles OpenID Connect discovery,
the authorization code exchange and ID token validation.

See the [Cognito authorization endpoint documentation](https://docs.aws.amazon.com/cognito/latest/developerguide/authorization-endpoint.html)
and [userInfo endpoint documentation](https://docs.aws.amazon.com/cognito/latest/developerguide/userinfo-endpoint.html).
