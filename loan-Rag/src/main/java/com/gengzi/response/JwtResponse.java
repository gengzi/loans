package com.gengzi.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private String id;
    private String username;

    public JwtResponse(String accessToken, String id, String username) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
    }
}
