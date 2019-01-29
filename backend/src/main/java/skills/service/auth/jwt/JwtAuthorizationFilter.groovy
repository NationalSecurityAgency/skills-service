package skills.service.auth.jwt

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthorizationFilter extends BasicAuthenticationFilter {
    @Autowired
    JwtHelper jwtHelper

    JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager)
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(JwtHelper.TOKEN_HEADER)

        if (header == null || !header.startsWith(JwtHelper.TOKEN_PREFIX)) {
            chain.doFilter(request, response)
            return
        }

        UsernamePasswordAuthenticationToken authentication = jwtHelper.getAuthentication(request)
        SecurityContextHolder.getContext().setAuthentication(authentication)

        chain.doFilter(request, response)
    }

}
