package skills.dbupgrade

import org.springframework.context.annotation.Conditional
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import javax.annotation.PostConstruct
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order()
@Conditional(DBUpgrade.InProgress)
class UpgradeInProgressFilter extends OncePerRequestFilter{

    @PostConstruct
    public void init() {
        //discover @Controller @DBUpgradeSafe urls

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, java.io.IOException {

    }
}
