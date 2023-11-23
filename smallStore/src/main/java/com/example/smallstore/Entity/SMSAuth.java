package com.example.smallstore.Entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "emailAuth")
public class SMSAuth {
    @ApiModelProperty(value = "id(auto)", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ApiModelProperty(value = "phone", example = "01012345678")
    @Column
    private String phone;

    @ApiModelProperty(value = "이메일 시간", example = "2023-08-22/01:00")
    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdDate;

    @ApiModelProperty(value = "인증 코드", example = "abc123")
    @Column
    private String randomCode;
}
