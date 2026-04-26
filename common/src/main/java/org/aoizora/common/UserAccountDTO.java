package org.aoizora.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccountDTO {
    private String id;
    private String name;
    private String email;
    private Role role;
}
