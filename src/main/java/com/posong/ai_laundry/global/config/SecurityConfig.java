package com.posong.ai_laundry.global.config;

import com.posong.ai_laundry.global.security.JwtAuthenticationFilter;
import com.posong.ai_laundry.global.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				// JWT 기반이라 세션은 사용하지 않는다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(authorize -> authorize
						// 회원가입, 로그인, 재발급은 인증 없이 열어둔다.
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs",
								"/v3/api-docs/**",
								"/v3/api-docs.yaml",
								"/actuator/health",
								"/actuator/health/**",
								"/actuator/prometheus",
								"/api/auth/signup/local",
								"/api/auth/login/local",
								"/api/auth/reissue"
						).permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 비밀번호는 BCrypt로 해시한다.
		return new BCryptPasswordEncoder();
	}
}
