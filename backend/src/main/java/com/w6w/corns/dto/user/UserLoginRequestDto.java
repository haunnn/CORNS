package com.w6w.corns.dto.user;

import com.w6w.corns.domain.user.User;
import io.swagger.annotations.ApiModel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(value="유저 로그인 요청 정보")
public class UserLoginRequestDto {

    private String email;
    private String password;

    public User toEntity(){
        return User
                .builder()
                .email(email)
                .password(password)
                .build();
    }
}
