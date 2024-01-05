package com.cydeo.client;

import com.cydeo.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/user/check/{username}")
    ResponseEntity<UserResponseDTO> checkByUserName(@RequestHeader(value = "Authorization") String authorizationHeader, @PathVariable("username") String username);

}
