package com.cydeo.controller;

import com.cydeo.dto.wrapper.ResponseWrapper;
import com.cydeo.dto.TaskDTO;
import com.cydeo.enums.Status;
import com.cydeo.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @RolesAllowed("Manager")
    @PostMapping("/create")
    public ResponseEntity<ResponseWrapper> createTask(@Valid @RequestBody TaskDTO taskDTO) {

        TaskDTO createdTask = taskService.create(taskDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED)
                        .message("Task is successfully created.")
                        .data(createdTask)
                        .build());

    }

    @RolesAllowed({"Manager", "Employee"})
    @GetMapping("/read/{taskCode}")
    public ResponseEntity<ResponseWrapper> getByTaskCode(@PathVariable("taskCode") String taskCode) {

        TaskDTO foundTask = taskService.readByTaskCode(taskCode);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Task is successfully retrieved.")
                        .data(foundTask)
                        .build());

    }

    @RolesAllowed("Manager")
    @GetMapping("/read/all/{projectCode}")
    public ResponseEntity<ResponseWrapper> getTasksByProject(@PathVariable("projectCode") String projectCode) {

        List<TaskDTO> foundTasks = taskService.readAllTasksByProject(projectCode);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Tasks are successfully retrieved.")
                        .data(foundTasks)
                        .build());

    }

    @RolesAllowed("Employee")
    @GetMapping("/read/employee/archive")
    public ResponseEntity<ResponseWrapper> employeeArchivedTasks() {

        List<TaskDTO> foundTasks = taskService.readAllByStatus(Status.COMPLETED);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Tasks are successfully retrieved.")
                        .data(foundTasks)
                        .build());

    }

    @RolesAllowed("Employee")
    @GetMapping("/read/employee/pending-tasks")
    public ResponseEntity<ResponseWrapper> employeePendingTasks() {

        List<TaskDTO> foundTasks = taskService.readAllByStatusIsNot(Status.COMPLETED);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Tasks are successfully retrieved.")
                        .data(foundTasks)
                        .build());

    }

    @RolesAllowed("Manager")
    @GetMapping("/count/project/{projectCode}")
    public ResponseEntity<ResponseWrapper> getCountsByProject(@PathVariable("projectCode") String projectCode) {

        Map<String, Integer> taskCounts = taskService.getCountsByProject(projectCode);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Task counts are successfully retrieved.")
                        .data(taskCounts)
                        .build());

    }

    @RolesAllowed("Admin")
    @GetMapping("/count/employee/{assignedEmployee}")
    public ResponseEntity<ResponseWrapper> getCountByAssignedEmployee(@PathVariable("assignedEmployee") String assignedEmployee) {

        Integer taskCount = taskService.getCountByAssignedEmployee(assignedEmployee);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Task count is successfully retrieved.")
                        .data(taskCount)
                        .build());

    }

    @RolesAllowed("Manager")
    @PutMapping("/update/{taskCode}")
    public ResponseEntity<ResponseWrapper> updateTask(@PathVariable("taskCode") String taskCode,
                                                      @Valid @RequestBody TaskDTO taskDTO) {

        TaskDTO updatedTask = taskService.update(taskCode, taskDTO);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Task is successfully updated.")
                        .data(updatedTask)
                        .build());

    }

    @RolesAllowed("Employee")
    @PutMapping("/update/employee/{taskCode}")
    public ResponseEntity<ResponseWrapper> employeeUpdateTasks(@PathVariable("taskCode") String taskCode,
                                                               @RequestParam Status status) {

        TaskDTO updatedTask = taskService.updateStatus(taskCode, status);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Task is successfully updated.")
                        .data(updatedTask)
                        .build());

    }

    @RolesAllowed("Manager")
    @PutMapping("/complete/project/{projectCode}")
    ResponseEntity<ResponseWrapper> completeByProject(@PathVariable("projectCode") String projectCode) {

        taskService.completeByProject(projectCode);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Tasks are successfully completed.")
                        .build());

    }

    @RolesAllowed("Manager")
    @DeleteMapping("/delete/{taskCode}")
    public ResponseEntity<Void> deleteTask(@PathVariable("taskCode") String taskCode) {
        taskService.delete(taskCode);
        return ResponseEntity.noContent().build();
    }

    @RolesAllowed("Manager")
    @DeleteMapping("/delete/project/{projectCode}")
    ResponseEntity<ResponseWrapper> deleteByProject(@PathVariable("projectCode") String projectCode) {

        taskService.deleteByProject(projectCode);

        return ResponseEntity
                .ok(ResponseWrapper.builder()
                        .success(true)
                        .statusCode(HttpStatus.OK)
                        .message("Tasks are successfully deleted.")
                        .build());

    }

}
