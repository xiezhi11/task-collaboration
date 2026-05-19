package com.taskcollab.config;

import com.taskcollab.common.UserContext;
import com.taskcollab.entity.SysUser;
import com.taskcollab.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/h2-console/**");
    }

    @Component
    public static class AuthInterceptor implements HandlerInterceptor {

        @Autowired
        private SysUserService sysUserService;

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String userIdStr = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            String userRole = request.getHeader("X-User-Role");

            if (userIdStr == null || username == null) {
                username = "leader1";
            }

            SysUser user = sysUserService.lambdaQuery().eq(SysUser::getUsername, username).one();
            if (user == null) {
                user = sysUserService.getById(2L);
            }

            UserContext.setUserId(user.getId());
            UserContext.setUserName(user.getName());
            UserContext.setUserRole(user.getRole());

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            UserContext.clear();
        }
    }
}
