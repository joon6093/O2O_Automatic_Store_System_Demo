package com.SJY.O2O_Automatic_Store_System_Demo.config.security;

import com.SJY.O2O_Automatic_Store_System_Demo.config.security.guard.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final MemberGuard memberGuard;
    private final PostGuard postGuard;
    private final CommentGuard commentGuard;
    private final MessageGuard messageGuard;
    private final MessageSenderGuard messageSenderGuard;
    private final MessageReceiverGuard messageReceiverGuard;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          MemberGuard memberGuard,
                          PostGuard postGuard,
                          CommentGuard commentGuard,
                          MessageGuard messageGuard,
                          MessageSenderGuard messageSenderGuard,
                          MessageReceiverGuard messageReceiverGuard) {
        this.userDetailsService = userDetailsService;
        this.memberGuard = memberGuard;
        this.postGuard = postGuard;
        this.commentGuard = commentGuard;
        this.messageGuard = messageGuard;
        this.messageSenderGuard = messageSenderGuard;
        this.messageReceiverGuard = messageReceiverGuard;
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/exception/**","/swagger-ui/**","/v3/api-docs/**");
    }

    @Bean
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public WebSecurityCustomizer configureH2ConsoleEnable() {
        return web -> web.ignoring().requestMatchers(PathRequest.toH2Console());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagementConfigurer ->
                        sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize ->
                        authorize
                        .requestMatchers(HttpMethod.POST, "/api/sign-in", "/api/sign-up","/api/refresh-token").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/members/{id}/**")
                                .access((authentication, context) -> new AuthorizationDecision(memberGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/{id}/**")
                            .access((authentication, context) -> new AuthorizationDecision(postGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/{id}/**")
                            .access((authentication, context) -> new AuthorizationDecision(postGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/{id}/**")
                            .access((authentication, context) -> new AuthorizationDecision(commentGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.GET, "/api/messages/sender", "/api/messages/receiver").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/messages/{id}")
                            .access((authentication, context) -> new AuthorizationDecision(messageGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.POST, "/api/messages").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/messages/sender/{id}")
                            .access((authentication, context) -> new AuthorizationDecision(messageSenderGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.DELETE, "/api/messages/receiver/{id}")
                            .access((authentication, context) -> new AuthorizationDecision(messageReceiverGuard.check(Long.parseLong(context.getVariables().get("id")))))
                        .requestMatchers(HttpMethod.GET, "/api/**","/image/**").permitAll()
                        .anyRequest().hasAnyRole("ADMIN"))
                .exceptionHandling((exceptionConfig) ->
                        exceptionConfig.authenticationEntryPoint(new CustomAuthenticationEntryPoint()).accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .addFilterBefore(new JwtAuthenticationFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
