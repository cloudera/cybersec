package com.cloudera.parserchains.queryservice.model.ranger;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CybersecUserDetails extends User {

    private String fullUsername;

    public CybersecUserDetails(String username, String fullUsername, String password,
                               Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.fullUsername = fullUsername;
    }

    public CybersecUserDetails(String username, String fullUsername, String password, boolean enabled, boolean accountNonExpired,
                               boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.fullUsername = fullUsername;
    }

}
