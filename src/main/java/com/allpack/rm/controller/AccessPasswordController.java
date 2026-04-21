package com.allpack.rm.controller;

import com.allpack.rm.dto.ApiResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AccessPasswordController {

    @Value("${app.access.password:}")
    private String accessPassword;

    @PostMapping("/session/password")
    public ApiResultDto verifyPassword(@RequestParam("password") String password) {
        if (!StringUtils.hasText(accessPassword)) {
            return ApiResultDto.fail("접근 비밀번호가 설정되지 않았습니다.");
        }
        if (!accessPassword.equals(password)) {
            return ApiResultDto.fail("비밀번호가 일치하지 않습니다.");
        }
        return ApiResultDto.success("ok");
    }
}
