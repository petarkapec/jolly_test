package hr.JollyBringer.JollyBringer.rest;

import hr.JollyBringer.JollyBringer.domain.Participant;
import hr.JollyBringer.JollyBringer.domain.Role;
import hr.JollyBringer.JollyBringer.service.EntityMissingException;
import hr.JollyBringer.JollyBringer.service.ParticipantService;
import hr.JollyBringer.JollyBringer.service.RequestDeniedException;
import hr.JollyBringer.JollyBringer.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.io.IOException;
import java.util.*;


import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = false)
public class WebSecurityBasic {


    @Value("${progi.frontend.url}")
    private String frontendUrl;

    private final ParticipantService participantService;
    private final RoleService roleService;



    public WebSecurityBasic(ParticipantService participantService, RoleService roleService) {
        this.participantService = participantService;
        this.roleService = roleService;



    }

    @Bean
    @Profile("basic-security")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        http.formLogin(withDefaults());
        http.httpBasic(withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Profile("oauth-security")
    public SecurityFilterChain oauthFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(
                            frontendUrl,
                            "https://jollybringer-frontend-latest.onrender.com"
                    ));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/check-auth").authenticated();
                    auth.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(this::oauth2AuthenticationSuccessHandler)
                        .userInfoEndpoint(
                                userInfoEndpoint -> userInfoEndpoint.userAuthoritiesMapper(this.authorityMapper()))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .exceptionHandling(handling -> handling.authenticationEntryPoint(new Http403ForbiddenEntryPoint()));

        return http.build();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(participantService);
    }
    private void oauth2AuthenticationSuccessHandler(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException {// Extract the OAuth2User details
       OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        if (authentication==null){
            System.out.println("Authentication is null");
        }
        else System.out.println("Authentication is NOT null");
        // You can retrieve user information like this
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // Save the user details to your database

        //TODO roles need to be in database on startup
        if(participantService.findByEmail(email).isEmpty()){

            if(roleService.findByName("Participant").isEmpty()){
                throw new EntityMissingException(Role.class, "Participant");
            }
            participantService.createParticipant(new Participant(name, email, roleService.findByName("Participant").get()));
        }

        System.out.println("Redirecting");
        httpServletResponse.sendRedirect(frontendUrl + "/dashboard");
    }

    @Bean
    @Profile("form-security")
    public SecurityFilterChain spaFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                .anyRequest().authenticated());
        http.formLogin(configurer -> {
                    configurer.successHandler((request, response, authentication) ->
                                    response.setStatus(HttpStatus.NO_CONTENT.value())
                            )
                            .failureHandler(new SimpleUrlAuthenticationFailureHandler());
                }
        );
        http.exceptionHandling(configurer -> {
            final RequestMatcher matcher = new NegatedRequestMatcher(
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML));
            configurer
                    .defaultAuthenticationEntryPointFor((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    }, matcher);
        });
        http.logout(configurer -> configurer
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpStatus.NO_CONTENT.value())));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Profile({ "basic-security", "form-security", "oauth-security" })
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(PathRequest.toH2Console());
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }



    @Bean
    public GrantedAuthoritiesMapper authorityMapper() {
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();

        authorityMapper.setDefaultAuthority("ROLE_PARTICIPANT");

        return authorityMapper;
    }
}
