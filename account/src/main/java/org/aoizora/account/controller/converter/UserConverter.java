package org.aoizora.account.controller.converter;

import jakarta.annotation.Nullable;
import org.aoizora.account.dao.domain.User;
import org.aoizora.account.dto.UserDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserConverter implements Converter<User, UserDTO> {
    @Override
    public @Nullable UserDTO convert(User source) {
        UserDTO target = new UserDTO();
        target.setId(source.getId());

        return target;
    }
}
