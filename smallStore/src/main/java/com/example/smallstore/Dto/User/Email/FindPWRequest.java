package com.example.smallstore.Dto.User.Email;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data // get, set 둘 다 됨.
@RequiredArgsConstructor
public class FindPWRequest {
    @ApiModelProperty(example="test")
    private String id;
    @ApiModelProperty(example="test@naver.com")
    private String email;
}
