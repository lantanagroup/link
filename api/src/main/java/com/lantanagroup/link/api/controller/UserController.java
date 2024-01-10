package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Hasher;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.User;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Autowired
  private SharedService sharedService;

  @InitBinder
  public void initializeBinder(WebDataBinder binder) {
    binder.setDisallowedFields("passwordSalt", "passwordHash");
  }

  @GetMapping("/{userId}")
  public  User getUser(@PathVariable UUID userId) {
    return this.sharedService.getUser(userId);
  }

  @GetMapping
  public List<User> searchUsers(@RequestParam(defaultValue = "false") boolean includeDisabled, @RequestParam String email) {
    return this.sharedService.searchUsers(includeDisabled, email);
  }

  private void saveUser(User user) {
    if (StringUtils.isEmpty(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
    }

    // Don't let the user's password or enabled status change via this operation
    if (user.getId() != null) {
      User found = this.sharedService.getUser(user.getId());

      if (found != null) {
        user.setPasswordSalt(found.getPasswordSalt());
        user.setPasswordHash(found.getPasswordHash());
        user.setEnabled(found.getEnabled());
      }
    } else {
      user.setPasswordSalt(null);
      user.setPasswordHash(null);
    }

    this.sharedService.saveUser(user);
  }

  @PostMapping
  public User createUser(@RequestBody User user) {
    user.setId(null);
    user.setEnabled(true);
    this.saveUser(user);
    return user;
  }

  @PostMapping("/$bulk")
  public OperationOutcome createBulkUsers(@RequestBody List<User> users) {
    OperationOutcome oo = new OperationOutcome();
    for (User user : users) {
      try {
        this.saveUser(user);
        oo.getIssue().add(new OperationOutcome.OperationOutcomeIssueComponent()
                .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setDiagnostics(String.format("Successfully created user with email %s", user.getEmail())));
      } catch (ResponseStatusException ex) {
        oo.getIssue().add(new OperationOutcome.OperationOutcomeIssueComponent()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(String.format("Error creating user with email %s: %s", user.getEmail(), ex.getReason())));
      } catch (Exception ex) {
        logger.error("Error creating user with email {}", user.getEmail(), ex);
        oo.getIssue().add(new OperationOutcome.OperationOutcomeIssueComponent()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(String.format("Error creating user with email %s", user.getEmail())));
      }
    }
    return oo;
  }

  @PutMapping("/{userId}")
  public User updateUser(@RequestBody User user, @PathVariable UUID userId) {
    User current = this.sharedService.getUser(userId);

    if (current == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User with id %s is not found", userId));
    }

    user.setId(userId);
    user.setPasswordHash(current.getPasswordHash());    // Don't change the password
    user.setPasswordSalt(current.getPasswordSalt());

    this.saveUser(user);

    user.setPasswordHash(null);                         // Don't return the password hash/salt in the response
    user.setPasswordSalt(null);
    return user;
  }

  @PutMapping("/{userId}/$change-password")
  public void updateUserPassword(@PathVariable UUID userId, @RequestBody String newPassword) {
    User user = this.sharedService.getUser(userId);

    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User %s is not found", userId));
    }

    try {
      byte[] salt = Hasher.getRandomSalt();
      user.setPasswordSalt(salt);
      user.setPasswordHash(Hasher.hash(newPassword, salt));
    } catch (Exception ex) {
      logger.error("Could not hash user {}'s new password", userId, ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    this.sharedService.saveUser(user);
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable UUID userId) {
    User user = this.sharedService.getUser(userId);

    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User %s not found", userId));
    }

    user.setEnabled(false);
    this.sharedService.saveUser(user);
  }
}
