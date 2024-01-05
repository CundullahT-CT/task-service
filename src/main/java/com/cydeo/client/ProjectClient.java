package com.cydeo.client;

import com.cydeo.dto.ProjectResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "project-service")
public interface ProjectClient {

    @GetMapping("/api/v1/project/read/manager/{projectCode}")
    ResponseEntity<ProjectResponseDTO> getManagerByProjectCode(@RequestHeader(value = "Authorization") String authorizationHeader, @PathVariable("projectCode") String projectCode);

    @GetMapping("/api/v1/project/check/{projectCode}")
    ResponseEntity<ProjectResponseDTO> checkByProjectCode(@RequestHeader(value = "Authorization") String authorizationHeader, @PathVariable("projectCode") String projectCode);

}
