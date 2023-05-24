package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Hasher;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.User;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Autowired
  private SharedService sharedService;

  @GetMapping
  public List<User> searchUsers(@RequestParam(defaultValue = "false") boolean includeDisabled) {
    return this.sharedService.searchUsers(includeDisabled);
  }

  private void saveUser(User user) {
    if (StringUtils.isEmpty(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
    }

    // Don't let the user's password or enabled status change via this operation
    if (StringUtils.isNotEmpty(user.getId())) {
      User found = this.sharedService.getUser(user.getId());

      if (found != null) {
        user.setPassword(found.getPassword());
        user.setEnabled(found.getEnabled());
      }
    } else {
      user.setPassword(null);
    }

    this.sharedService.saveUser(user);
  }

  @PostMapping
  public User createUser(@RequestBody User user) {
    user.setId((new ObjectId()).toString());
    user.setEnabled(true);
    this.saveUser(user);
    return user;
  }

  @PutMapping("/{userId}")
  public User updateUser(@RequestBody User user, @PathVariable String userId) {
    if (StringUtils.isNotEmpty(userId) && this.sharedService.getUser(userId) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User with id %s is not found", userId));
    }

    user.setId(userId);
    this.saveUser(user);
    return user;
  }

  @PutMapping("/{userId}/$change-password")
  public void updateUserPassword(@PathVariable String userId, @RequestBody String newPassword) {
    User user = this.sharedService.getUser(userId);

    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User %s is not found", userId));
    }

    try {
      user.setPassword(Hasher.hash(newPassword));
    } catch (Exception ex) {
      logger.error("Could not hash user {}'s new password", userId, ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    this.sharedService.saveUser(user);
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable String userId) {
    User user = this.sharedService.getUser(userId);

    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User %s not found", userId));
    }

    user.setEnabled(false);
    this.sharedService.saveUser(user);
  }
}
