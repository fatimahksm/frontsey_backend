package com.dbwb.platform.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Small convenience accessor so services don't repeat SecurityContext plumbing. */
@Component
public class CurrentAccount {

    public AuthenticatedAccount get() {
        return (AuthenticatedAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
