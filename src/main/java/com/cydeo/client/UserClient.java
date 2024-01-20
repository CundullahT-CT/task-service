package com.cydeo.client;

import com.cydeo.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/user/check/{username}")
    ResponseEntity<UserResponse> checkByUserName(@RequestHeader(name = "Authorization") String authorizationHeader, @PathVariable("username") String username);

}
