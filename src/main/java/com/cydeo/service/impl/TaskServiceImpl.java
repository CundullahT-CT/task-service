package com.cydeo.service.impl;

import com.cydeo.client.ProjectClient;
import com.cydeo.client.UserClient;
import com.cydeo.dto.ProjectResponseDTO;
import com.cydeo.dto.TaskDTO;
import com.cydeo.dto.UserResponseDTO;
import com.cydeo.entity.Task;
import com.cydeo.enums.Status;
import com.cydeo.exception.*;
import com.cydeo.repository.TaskRepository;
import com.cydeo.service.KeycloakService;
import com.cydeo.service.TaskService;
import com.cydeo.util.MapperUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final MapperUtil mapperUtil;
    private final ProjectClient projectClient;
    private final UserClient userClient;
    private final KeycloakService keycloakService;

    public TaskServiceImpl(TaskRepository taskRepository, MapperUtil mapperUtil, ProjectClient projectClient, UserClient userClient, KeycloakService keycloakService) {
        this.taskRepository = taskRepository;
        this.mapperUtil = mapperUtil;
        this.projectClient = projectClient;
        this.userClient = userClient;
        this.keycloakService = keycloakService;
    }


    @Override
    public TaskDTO create(TaskDTO taskDTO) {

        Optional<Task> foundTask = taskRepository.findByTaskCode(taskDTO.getTaskCode());

        if (foundTask.isPresent()) {
            throw new TaskAlreadyExistsException("Task already exists.");
        }

        checkProjectExists(taskDTO.getProjectCode());
        checkManagerAccess(keycloakService.getUsername(), taskDTO.getProjectCode());
        checkEmployeeExists(taskDTO.getAssignedEmployee());

        Task taskToSave = mapperUtil.convert(taskDTO, new Task());
        taskToSave.setTaskStatus(Status.OPEN);
        taskToSave.setAssignedDate(LocalDate.now());

        Task savedTask = taskRepository.save(taskToSave);

        return mapperUtil.convert(savedTask, new TaskDTO());

    }

    @Override
    public TaskDTO readByTaskCode(String taskCode) {

        Task task = taskRepository.findByTaskCode(taskCode)
                .orElseThrow(() -> new TaskNotFoundException("Task does not exist."));

        checkAccess(task);

        return mapperUtil.convert(task, new TaskDTO());

    }

    @Override
    public List<TaskDTO> readAllTasksByProject(String projectCode) {
        checkManagerAccess(keycloakService.getUsername(), projectCode);
        List<Task> list = taskRepository.findAllByProjectCode(projectCode);
        return list.stream().map(obj -> mapperUtil.convert(obj, new TaskDTO())).collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> readAllByStatus(Status status) {

        String loggedInUserUsername = keycloakService.getUsername();

        List<Task> tasks = taskRepository.findAllByTaskStatusAndAssignedEmployee(status, loggedInUserUsername);

        return tasks.stream().map(task -> mapperUtil.convert(task, new TaskDTO())).collect(Collectors.toList());

    }

    @Override
    public List<TaskDTO> readAllByStatusIsNot(Status status) {

        String loggedInUserUsername = keycloakService.getUsername();

        List<Task> list = taskRepository.findAllByTaskStatusIsNotAndAssignedEmployee(status, loggedInUserUsername);

        return list.stream().map(obj -> mapperUtil.convert(obj, new TaskDTO())).collect(Collectors.toList());

    }

    @Override
    public Map<String, Integer> getCountsByProject(String projectCode) {

        checkManagerAccess(keycloakService.getUsername(), projectCode);

        int completedTaskCount = taskRepository.totalCompletedTasks(projectCode);
        int nonCompletedTaskCount = taskRepository.totalNonCompletedTasks(projectCode);

        Map<String, Integer> taskCounts = new HashMap<>();

        taskCounts.put("completedTaskCount", completedTaskCount);
        taskCounts.put("nonCompletedTaskCount", nonCompletedTaskCount);

        return taskCounts;

    }

    @Override
    public Integer getCountByAssignedEmployee(String assignedEmployee) {
        checkEmployeeExists(assignedEmployee);
        return taskRepository.countByAssignedEmployee(assignedEmployee);
    }

    @Override
    public TaskDTO update(String taskCode, TaskDTO taskDTO) {

        Task foundTask = taskRepository.findByTaskCode(taskCode)
                .orElseThrow(() -> new TaskNotFoundException("Task does not exist."));

        checkEmployeeExists(taskDTO.getAssignedEmployee());
        checkManagerAccess(keycloakService.getUsername(), foundTask.getProjectCode());
        checkProjectExists(taskDTO.getProjectCode());

        Task taskToUpdate = mapperUtil.convert(taskDTO, new Task());

        taskToUpdate.setId(foundTask.getId());
        taskToUpdate.setTaskCode(taskCode);
        taskToUpdate.setTaskStatus(taskDTO.getTaskStatus() == null ? foundTask.getTaskStatus() : taskDTO.getTaskStatus());
        taskToUpdate.setAssignedDate(LocalDate.now());

        Task updatedTask = taskRepository.save(taskToUpdate);

        return mapperUtil.convert(updatedTask, new TaskDTO());

    }

    @Override
    public TaskDTO updateStatus(String taskCode, Status status) {

        Task foundTask = taskRepository.findByTaskCode(taskCode)
                .orElseThrow(() -> new TaskNotFoundException("Task does not exist."));

        checkEmployeeAccess(keycloakService.getUsername(), foundTask);

        foundTask.setTaskStatus(status);

        Task updatedTask = taskRepository.save(foundTask);

        return mapperUtil.convert(updatedTask, new TaskDTO());

    }

    @Override
    public void completeByProject(String projectCode) {

        checkManagerAccess(keycloakService.getUsername(), projectCode);

        List<Task> tasks = taskRepository.findAllByProjectCode(projectCode);

        tasks.forEach(task -> {
            task.setTaskStatus(Status.COMPLETED);
            taskRepository.save(task);
        });

    }

    @Override
    public void delete(String taskCode) {

        Task taskToDelete = taskRepository.findByTaskCode(taskCode)
                .orElseThrow(() -> new TaskNotFoundException("Task does not exist."));

        checkManagerAccess(keycloakService.getUsername(), taskToDelete.getProjectCode());

        taskToDelete.setIsDeleted(true);
        taskToDelete.setTaskCode(taskCode + "-" + taskToDelete.getId());

        taskRepository.save(taskToDelete);

    }

    @Override
    public void deleteByProject(String projectCode) {

        checkManagerAccess(keycloakService.getUsername(), projectCode);

        List<Task> tasks = taskRepository.findAllByProjectCode(projectCode);

        tasks.forEach(taskToDelete -> {
            taskToDelete.setIsDeleted(true);
            taskToDelete.setTaskCode(taskToDelete.getTaskCode() + "-" + taskToDelete.getId());
            taskRepository.save(taskToDelete);
        });

    }

    private void checkProjectExists(String projectCode) {

        String accessToken = keycloakService.getAccessToken();

        ResponseEntity<ProjectResponseDTO> response = projectClient.checkByProjectCode(accessToken, projectCode);

        if (!Objects.requireNonNull(response.getBody()).isSuccess()) {
            throw new ProjectCheckFailedException("Project check is failed.");
        }

        if (!Objects.requireNonNull(response.getBody()).getData().equals(true)) {
            throw new ProjectNotFoundException("Project does not exist.");
        }

    }

    private void checkEmployeeExists(String assignedEmployee) {

        String accessToken = keycloakService.getAccessToken();

        if (!keycloakService.hasClientRole(assignedEmployee, "Employee")) {
            throw new EmployeeNotFoundException("User is not an employee.");
        }

        ResponseEntity<UserResponseDTO> response = userClient.checkByUserName(accessToken, assignedEmployee);

        if (!Objects.requireNonNull(response.getBody()).isSuccess()) {
            throw new EmployeeCheckFailedException("Employee check is failed.");
        }

        if (!Objects.requireNonNull(response.getBody()).getData().equals(true)) {
            throw new EmployeeNotFoundException("Employee does not exist.");
        }

    }

    private void checkAccess(Task task) {

        String loggedInUserUsername = keycloakService.getUsername();

        if (keycloakService.hasClientRole(loggedInUserUsername, "Manager")) {
            checkManagerAccess(loggedInUserUsername, task.getProjectCode());
        } else if (keycloakService.hasClientRole(loggedInUserUsername, "Employee")) {
            checkEmployeeAccess(loggedInUserUsername, task);
        } else {
            throw new TaskAccessDeniedException("Access denied.");
        }

    }

    private void checkManagerAccess(String loggedInUserUsername, String projectCode) {

        String accessToken = keycloakService.getAccessToken();

        ResponseEntity<ProjectResponseDTO> response = projectClient.getManagerByProjectCode(accessToken, projectCode);

        if (Objects.requireNonNull(response.getBody()).isSuccess()) {
            String taskManager = (String) response.getBody().getData();
            if (!loggedInUserUsername.equals(taskManager)) {
                throw new TaskAccessDeniedException("Access denied, make sure that you are working on your own project.");
            }
        } else {
            throw new ManagerNotRetrievedException("Manager cannot be retrieved.");
        }

    }

    private void checkEmployeeAccess(String loggedInUserUsername, Task task) {

        String taskEmployee = task.getAssignedEmployee();

        if (!loggedInUserUsername.equals(taskEmployee)) {
            throw new TaskAccessDeniedException("Access denied, make sure that you are working on your own task.");
        }

    }

}
