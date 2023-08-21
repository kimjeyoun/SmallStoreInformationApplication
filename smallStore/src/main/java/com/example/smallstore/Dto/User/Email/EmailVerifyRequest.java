package com.example.smallstore.Dto.User.Email;

import com.example.smallstore.enums.VerifyRole;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data // get, set 둘 다 됨.
@RequiredArgsConstructor
public class EmailVerifyRequest {
    private String email, randomCode;
    private VerifyRole verifyRole;
}
