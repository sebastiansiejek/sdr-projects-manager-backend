package sdrprojectsmanager.sdr.teams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sdrprojectsmanager.sdr.exception.ResourceNotFoundException;
import sdrprojectsmanager.sdr.utils.ApiResponses.ApiResponse;

import javax.validation.Valid;

@RestController
@ControllerAdvice()
@Valid
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/teams")
public class TeamController {

    @Autowired
    private TeamsRepository teamsRepository;

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        Team searchResult = teamsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getAll() {
        Iterable<Team> allTeams = teamsRepository.findAll();
        if (allTeams.equals(null))
            throw new ResourceNotFoundException("Teams not found");
        return ResponseEntity.ok(allTeams);
    }

    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody Object add(@Valid @RequestBody Team newTeam) {
        Team team = new Team();
        try {
            team.setName(newTeam.getName());
            team.setMaxPeople(newTeam.getMaxPeople());
            teamsRepository.save(team);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Create team fails");
        }
        return ResponseEntity.ok(team);
    }

    @RequestMapping(value = "/edit/{teamId}", method = RequestMethod.POST)
    public @ResponseBody Object edit(@PathVariable Integer teamId, @RequestBody Team editTeam) {
        Team teamEdit = teamsRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        try {
            teamEdit.setName(editTeam.getName());
            teamEdit.setMaxPeople(editTeam.getMaxPeople());
            teamsRepository.save(teamEdit);
        } catch (DataAccessException e) {
            throw new ResourceNotFoundException(e.getCause().getMessage());
        }
        return ResponseEntity.ok(teamEdit);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> editTask(@PathVariable Integer id) {
        Team team = teamsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        teamsRepository.deleteById(id);

        return ApiResponse.delete(team, "Team has been deleted");
    }
}
