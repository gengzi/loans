package com.gengzi.controller;


import com.gengzi.request.UserLoginReq;
import com.gengzi.response.JwtResponse;
import com.gengzi.response.Result;
import com.gengzi.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Tag(name = "用户管理", description = "用户管理")
public class UserController {


    @Autowired
    private UserService userService;

    @PostMapping("/user/login")
    @ResponseBody
    public Result<?> authenticateUser(@Valid @RequestBody UserLoginReq loginRequest) {
        JwtResponse login = userService.login(loginRequest);
        return Result.success(login);
    }


}
