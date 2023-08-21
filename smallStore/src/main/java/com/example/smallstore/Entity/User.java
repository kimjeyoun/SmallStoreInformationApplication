package com.example.smallstore.Entity;

import com.example.smallstore.Dto.User.Email.UpdatePWRequest;
import com.example.smallstore.enums.UserRole;
import com.example.smallstore.Dto.User.UserUpdateRequest;
import com.example.smallstore.enums.VerifyRole;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Data
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {
    @ApiModelProperty(value = "id", example = "test")
    @Id
    @Column(unique = true, nullable = false)
    private String id;

    @ApiModelProperty(value = "email", example = "test@naver.com")
    @Column(nullable = false)
    private String email;

    @ApiModelProperty(value = "password", example = "test")
    @Column(nullable = false)
    private String password;

    @ApiModelProperty(value = "address", example = "경기도 군포시")
    @Column(unique = true, nullable = false)
    private String address;

    @ApiModelProperty(value = "nickname", example = "test")
    @Column(nullable = false)
    private String nickname;

    // 이메일 2차 인증
    @ApiModelProperty(value = "이메일 인증 확인", example = "T")
    @Column(nullable = false)
    @Type(type = "true_false")
    private boolean emailConfirmed;

    // 유저 권한
    @ApiModelProperty(value = "유저 권한", example = "USER/SHOPOWNER")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    // 비밀번호 인증 확인
    @ApiModelProperty(value = "비밀번호 인증 권한", example = "VERIFYTRUE/VERIFYFALSE")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerifyRole verifyRole;

    public void update(UserUpdateRequest userUpdateRequest) {
        this.email = userUpdateRequest.getEmail();
        this.address = userUpdateRequest.getAddress();
        this.nickname = userUpdateRequest.getNickname();
    }

    public void updatePW(UpdatePWRequest updatePWRequest) {
        this.email = updatePWRequest.getEmail();
        this.password = updatePWRequest.getPassword();
        this.verifyRole = updatePWRequest.getVerifyRole();
    }

}
