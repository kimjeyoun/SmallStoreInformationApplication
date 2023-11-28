package com.example.smallstore.Service;

import com.example.smallstore.Dto.ResponseDto;
import com.example.smallstore.Dto.User.*;
import com.example.smallstore.Dto.User.SMS.SMSVerifyRequest;
import com.example.smallstore.Dto.User.SMS.UpdatePWRequest;
import com.example.smallstore.Entity.User;
import com.example.smallstore.JWT.JwtTokenProvider;
import com.example.smallstore.Repository.UserRepository;
import com.example.smallstore.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static com.example.smallstore.Error.ErrorCode.ACCESS_DENIED_EXCEPTION;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SMSService messageService;

    @Value("${kakao.pw}")
    private String kakaoPW;

    // 토큰 헤더에 저장
    public void setJwtTokenInHeader(String id, HttpServletResponse response, HttpServletRequest request) {
        UserRole userRole = userRepository.findById(id).orElseThrow().getUserRole();

        String accessToken = jwtTokenProvider.createAccessToken(id, userRole);
        String refreshToken = jwtTokenProvider.createRefreshToken(id, userRole);

        jwtTokenProvider.setHeaderAccessToken(response, accessToken);
        jwtTokenProvider.setHeaderRefreshToken(response, refreshToken);

        String ip = this.getClientIp(request);
        refreshTokenService.saveRefreshToken(id, ip, refreshToken);
    }

    // 회원가입
    public ResponseEntity signup(UserSignupRequest userSignupRequest, HttpServletResponse response, HttpServletRequest request) {
        // 아이디가 있으면 에러 던짐.
        if (userRepository.existsById(userSignupRequest.getId())) {
            return ResponseEntity.badRequest().body(ResponseDto.failRes(400, "회원가입 실패/중복된 아이디 존재"));
        }
        // 비밀번호 저장
        userSignupRequest.setPassword(passwordEncoder.encode(userSignupRequest.getPassword()));
        if(userSignupRequest.getLoginType().equals("kakaoLogin")){
            userSignupRequest.setPassword(passwordEncoder.encode(kakaoPW));
            userSignupRequest.setSecondConfirmed(true);
        } else {
            // 2차 인증 이메일 해야함.
            userSignupRequest.setSecondConfirmed(false);
        }
        userRepository.save(userSignupRequest.toEntity());
        this.setJwtTokenInHeader(userSignupRequest.getId(), response, request);
        return ResponseEntity.ok(ResponseDto.successRes(200, "회원가입 성공"));
    }

    // 로그인
    public ResponseEntity login(UserLoginRequest userLoginRequest, HttpServletResponse response, HttpServletRequest request){
        if(!userRepository.existsById(userLoginRequest.getId())){ // id가 존재하지 않으면
            return ResponseEntity.badRequest().body(ResponseDto.failRes(400, "로그인 실패/id 없음"));
        }
        User user = userRepository.findById(userLoginRequest.getId()).orElseThrow();

        if(!userLoginRequest.getLoginType().equals("ROLE_KAKAO")){
            if(!passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword())){ // 비밀번호 틀리면
                return ResponseEntity.badRequest().body(ResponseDto.failRes(400, "로그인 실패/비번 틀림"));
            }
            if(!user.isSecondConfirmed()){
                return ResponseEntity.badRequest().body(ResponseDto.failRes(400, "로그인 실패/2차 인증 안됨"));
            }
        }

        this.setJwtTokenInHeader(userLoginRequest.getId(), response, request);

        // 로그인 성공 시
        return ResponseEntity.ok(ResponseDto.res(200, "로그인 성공", user));
    }

    // 마이페이지 수정
    public ResponseEntity updateUser(UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(userUpdateRequest.getId()).orElseThrow();
        user.update(userUpdateRequest);
        userRepository.save(user);
        return ResponseEntity.ok(ResponseDto.res(200, "마이페이지 수정 성공", user));
    }

    // 로그아웃
    public ResponseEntity logout(HttpServletRequest request) {
        refreshTokenService.deleteToken(jwtTokenProvider.resolveRefreshToken(request));
        jwtTokenProvider.expireToken(jwtTokenProvider.resolveAccessToken(request));
        return ResponseEntity.ok(ResponseDto.successRes(200, "마이페이지 수정 성공"));
    }

    // 탈퇴
    // 카카오 탈퇴는 어떻게 할지 생각해야함.
    public ResponseEntity deleteUser(UserDeleteRequest userDeleteRequest, HttpServletRequest request) {
        User user = this.findUserByToken(request);
        if(passwordEncoder.matches(userDeleteRequest.getPassword(), user.getPassword())){
            userRepository.deleteById(user.getId());
            refreshTokenService.deleteToken(jwtTokenProvider.resolveRefreshToken(request));
            return ResponseEntity.ok(ResponseDto.successRes(200, "탈퇴 성공"));
        }
        return ResponseEntity.badRequest().body(ResponseDto.failRes(400, "탈퇴 실패/비번 틀림"));
    }

    // 2차 인증 실행
    public ResponseEntity secondConfirmed(UserConfirmedRequest userConfirmedRequest) {
        User user = userRepository.findById(userConfirmedRequest.getId()).orElseThrow();
        if(user == null){
            return  ResponseEntity.badRequest().body(ResponseDto.failRes(400, "2차 인증 문자 보내기 실패/회원가입이 제대로 되지 않음"));
        }
        messageService.sendMessage(userConfirmedRequest.getPhone());
        return ResponseEntity.ok(ResponseDto.successRes(200, "2차 인증 문자 보내기 성공"));
    }

    // 비밀번호 찾기 실행
    public ResponseEntity findPW(FindPWRequest findPWRequest) {
        User user = userRepository.findById(findPWRequest.getId()).orElseThrow();
        if(user == null){
            return  ResponseEntity.badRequest().body(ResponseDto.failRes(400, "비밀번호 찾기 실패/id 혹은 전화번호가 없음."));
        }
        messageService.sendMessage(findPWRequest.getPhone());
        return ResponseEntity.ok(ResponseDto.successRes(200, "비밀번호 찾기 문자 보내기 성공"));
    }

    // 인증 확인 및 db update
    public ResponseEntity verifyCode(SMSVerifyRequest smsVerifyRequest) {
        ResponseEntity response = messageService.verifySMS(smsVerifyRequest.getPhone(), smsVerifyRequest.getRandomCode());

        if(response.getStatusCode().equals(HttpStatus.OK) && smsVerifyRequest.getType().equals("auth")){
            User user = userRepository.findById(smsVerifyRequest.getUser_id()).orElseThrow();
                user.setSecondConfirmed(true);
                userRepository.save(user);
        }
        return response;
    }

    // 비밀번호 변경
    public ResponseEntity updatePW(UpdatePWRequest updatePWRequest){
        User user = userRepository.findByPhone(updatePWRequest.getPhone()).orElseThrow();
        if(user.getLoginType().equals("ROLE_KAKAO")){
            ResponseEntity.badRequest().body(ResponseDto.failRes(400, "비번 변경 실패/카카오로그인이라 비번 변경 불가"));
        }
        updatePWRequest.setPassword(passwordEncoder.encode(updatePWRequest.getPassword()));
        user.updatePW(updatePWRequest);
        userRepository.save(user);
        return ResponseEntity.ok(ResponseDto.successRes(200, "비밀번호 변경 성공"));
    }

    // 토큰에서 정보 가져오기
    public User findUserByToken(HttpServletRequest request) {
        String id = jwtTokenProvider.getUserId(jwtTokenProvider.resolveAccessToken(request));
        User user = userRepository.findById(id).orElseThrow();
        return user;
    }

    // ip 가져오기
    public static String getClientIp(HttpServletRequest request) {
        String clientIp = null;
        boolean isIpInHeader = false;

        List<String> headerList = new ArrayList<>();
        headerList.add("X-Forwarded-For");
        headerList.add("HTTP_CLIENT_IP");
        headerList.add("HTTP_X_FORWARDED_FOR");
        headerList.add("HTTP_X_FORWARDED");
        headerList.add("HTTP_FORWARDED_FOR");
        headerList.add("HTTP_FORWARDED");
        headerList.add("Proxy-Client-IP");
        headerList.add("WL-Proxy-Client-IP");
        headerList.add("HTTP_VIA");
        headerList.add("IPV6_ADR");

        for (String header : headerList) {
            clientIp = request.getHeader(header);
            if (StringUtils.hasText(clientIp) && !clientIp.equals("unknown")) {
                isIpInHeader = true;
                break;
            }
        }
        if (!isIpInHeader) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}
