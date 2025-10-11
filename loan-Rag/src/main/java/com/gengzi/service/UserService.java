package com.gengzi.service;


import com.gengzi.request.UserLoginReq;
import com.gengzi.response.JwtResponse;

public interface UserService {


    JwtResponse login(UserLoginReq loginRequest);

}
