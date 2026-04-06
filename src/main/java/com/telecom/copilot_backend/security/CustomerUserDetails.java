package com.telecom.copilot_backend.security;

import com.telecom.copilot_backend.entity.Customer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomerUserDetails implements UserDetails {

    @Getter
    private final String customerId;

    @Getter
    private final String name;

    private final String email;
    private final String password;

    public CustomerUserDetails(Customer customer) {
        this.customerId = customer.getCustomerId();
        this.name = customer.getName();
        this.email = customer.getEmail();
        this.password = customer.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** Email is the login username. */
    @Override
    public String getUsername() {
        return email;
    }
}

