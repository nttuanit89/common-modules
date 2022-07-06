package com.learning.common.restapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
//  @Autowired private JwtRequestFilter jwtRequestFilter;
//
//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//    http.csrf()
//        .disable()
//        .cors()
//        .disable()
//        .authorizeRequests()
//        .antMatchers()
//        .permitAll()
//        .antMatchers("/**")
//        .permitAll();
//    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//  }
}
