package sdrprojectsmanager.sdr.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sdrprojectsmanager.sdr.exception.ResourceNotFoundException;
import sdrprojectsmanager.sdr.projects.Project;
import sdrprojectsmanager.sdr.projects.ProjectsRepository;
import sdrprojectsmanager.sdr.users.User;
import sdrprojectsmanager.sdr.users.UsersRepository;
import sdrprojectsmanager.sdr.utils.PrincipalRole;
import sdrprojectsmanager.sdr.utils.ApiResponses.ApiResponse;

import javax.persistence.EntityManager;
import javax.validation.Valid;

@RestController
@ControllerAdvice()
@Valid
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectsRepository projectsRepository;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private EntityManager em;

    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Object getAll() {
        Iterable<Task> allTasks = taskRepository.findAll();
        if (allTasks.equals(null))
            throw new ResourceNotFoundException("Tasks not found");
        return ResponseEntity.ok(allTasks);
    }

    @RequestMapping(value = "/taskInProject/{projectId}", method = RequestMethod.GET)
    public ResponseEntity<?> getTaskInProject(@PathVariable Integer projectId) {

        Project project = projectsRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Iterable<Task> searchResult = taskRepository.findByProject(project);
        if (searchResult.equals(null))
            throw new ResourceNotFoundException("Tasks not found");

        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(value = "/userTask/{userId}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserTask(@PathVariable Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task for user not found with id: " + userId));

        Iterable<Task> searchResult = taskRepository.findByUserId(user);
        if (searchResult.equals(null))
            throw new ResourceNotFoundException("Tasks not found");

        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        Task searchResult = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return ResponseEntity.ok(searchResult);
    }

    @Procedure(name = "AddTask")
    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody Object add(@Valid @RequestBody AddTask newTask, Authentication authentication) {
        Integer taskId;

        try {
            taskId = (Integer) em.createNamedStoredProcedureQuery("AddTask")
                    .setParameter("task_name", newTask.getName())
                    .setParameter("task_description", newTask.getDescription())
                    .setParameter("user_id", (int) PrincipalRole.getFormatedRole(authentication).get("user_id"))
                    .setParameter("project_id", (int) newTask.getProjectId())
                    .setParameter("task_cost", newTask.getCost()).getOutputParameterValue("new_task_id");
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getCause().getMessage());
        }

        String message;

        if (taskId < 0) {

            if (taskId == -3)
                message = "Project has been closed";
            else if (taskId == -2)
                message = "Budget has been exceeded";
            else
                message = "Data is incorrenct";

            return ApiResponse.procedure(message);
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Dane niepoprawne"));

        return ApiResponse.delete(task, "Task has been created");
    }

    @RequestMapping(value = "/endTask/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> endTask(@PathVariable Integer id) {
        Task searchResult = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        searchResult.setState(1);
        taskRepository.save(searchResult);
        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> edit(@PathVariable Integer id, @RequestBody AddTask editTask) {
        Task searchResult = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!editTask.getName().isEmpty()) {
            searchResult.setName(editTask.getName());
        }

        if (!editTask.getDescription().isEmpty()) {
            searchResult.setDescription(editTask.getDescription());
        }

        if (editTask.getCost() > 0) {
            try {
                em.createNamedStoredProcedureQuery("EditTaskCost")
                        .setParameter("task_id", id)
                        .setParameter("task_cost", editTask.getCost()).execute();
            } catch (Exception e) {
                throw new ResourceNotFoundException(e.getCause().getMessage());
            }
        }
        searchResult.setState(editTask.getState());

        taskRepository.save(searchResult);

        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        taskRepository.deleteById(id);

        return ApiResponse.delete(task, "Task has been deleted");
    }
}
