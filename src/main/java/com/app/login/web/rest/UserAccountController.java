package com.app.login.web.rest;

import com.app.login.config.Constants;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.login.domain.User;
import com.app.login.repository.UserRepository;
import com.app.login.security.SecurityUtils;
import com.app.login.service.MailService;
import com.app.login.service.UserService;
import com.app.login.service.dto.UserDTO;
import com.app.login.web.rest.util.HeaderUtil;
import com.app.login.web.rest.vm.KeyAndPasswordVM;
import com.app.login.web.rest.vm.ManagedUserVM;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class UserAccountController {

    private final Logger log = LoggerFactory.getLogger(UserAccountController.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    public UserAccountController(UserRepository userRepository, UserService userService, MailService mailService) {

        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
    }

    /**
     * POST /register : register the user.
     *
     * @param managedUserVM
     *            the managed user View Model
     * @param request
     * @return the ResponseEntity with status 201 (Created) if the user is
     *         registered or 400 (Bad Request) if the login or email is already
     *         in use
     */
    @PostMapping(path = "/register", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM, HttpServletRequest request) {

        HttpHeaders textPlainHeaders = new HttpHeaders();
        textPlainHeaders.setContentType(MediaType.TEXT_PLAIN);

        log.info("getting remote ip = " + request.getRemoteAddr());
        String ipAddress = request.getRemoteAddr();

        Instant instant = Instant.now();

        List<User> usersByIpAndRegistrationDate = userRepository.findAllByIpAddressAndCreatedDateBetween(ipAddress, instant.minus(1, ChronoUnit.DAYS), instant);
        if (!usersByIpAndRegistrationDate.isEmpty()) {
            if (usersByIpAndRegistrationDate.size() == Constants.DOS_MAX_REGISTRATION_ALLOWED) {
                return new ResponseEntity<>("You cannot register", textPlainHeaders, HttpStatus.BAD_REQUEST);
            }
        }

        return userRepository.findOneByLogin(managedUserVM.getLogin()
            .toLowerCase())
            .map(user -> new ResponseEntity<>("login already in use", textPlainHeaders, HttpStatus.BAD_REQUEST))
            .orElseGet(() -> userRepository.findOneByEmail(managedUserVM.getEmail())
                .map(user -> new ResponseEntity<>("email address already in use", textPlainHeaders, HttpStatus.BAD_REQUEST))
                .orElseGet(() -> {
                    User user = userService.createUser(managedUserVM.getLogin(), managedUserVM.getPassword(), managedUserVM.getFirstName(), managedUserVM.getLastName(), managedUserVM.getEmail()
                        .toLowerCase(), managedUserVM.getImageUrl(), managedUserVM.getLangKey(), Instant.now(), ipAddress);

                    mailService.sendActivationEmail(user);
                    return new ResponseEntity<>(HttpStatus.CREATED);
                }));
    }

    /**
     * GET /activate : activate the registered user.
     *
     * @param key
     *            the activation key
     * @return the ResponseEntity with status 200 (OK) and the activated user in
     *         body, or status 500 (Internal Server Error) if the user couldn't
     *         be activated
     */
    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam(value = "key") String key) {
        return userService.activateRegistration(key)
            .map(user -> new ResponseEntity<String>(HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * GET /authenticate : check if the user is authenticated, and return its
     * login.
     *
     * @param request
     *            the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET /account : get the current user.
     *
     * @return the ResponseEntity with status 200 (OK) and the current user in
     *         body, or status 500 (Internal Server Error) if the user couldn't
     *         be returned
     */
    @GetMapping("/account")
    public ResponseEntity<UserDTO> getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(user -> new ResponseEntity<>(new UserDTO(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * POST /account : update the current user information.
     *
     * @param userDTO
     *            the current user information
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad
     *         Request) or 500 (Internal Server Error) if the user couldn't be
     *         updated
     */
    @PostMapping("/account")
    public ResponseEntity saveAccount(@Valid @RequestBody UserDTO userDTO) {
        final String userLogin = SecurityUtils.getCurrentUserLogin();
        Optional<User> existingUser = userRepository.findOneByEmail(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get()
            .getLogin()
            .equalsIgnoreCase(userLogin))) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert("user-management", "emailexists", "Email already in use"))
                .body(null);
        }
        return userRepository.findOneByLogin(userLogin)
            .map(u -> {
                userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(), userDTO.getLangKey(), userDTO.getImageUrl());
                return new ResponseEntity(HttpStatus.OK);
            })
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * POST /account/change_password : changes the current user's password
     *
     * @param password
     *            the new password
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad
     *         Request) if the new password is not strong enough
     */
    @PostMapping(path = "/account/change_password", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity changePassword(@RequestBody String password) {
        if (!checkPasswordLength(password)) {
            return new ResponseEntity<>("Incorrect password", HttpStatus.BAD_REQUEST);
        }
        userService.changePassword(password);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account/reset_password/init : Send an email to reset the password
     * of the user
     *
     * @param mail
     *            the mail of the user
     * @return the ResponseEntity with status 200 (OK) if the email was sent, or
     *         status 400 (Bad Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset_password/init", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity requestPasswordReset(@RequestBody String mail) {
        return userService.requestPasswordReset(mail)
            .map(user -> {
                mailService.sendPasswordResetMail(user);
                return new ResponseEntity<>("email was sent", HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>("email address not registered", HttpStatus.BAD_REQUEST));
    }

    /**
     * POST /account/reset_password/finish : Finish to reset the password of the
     * user
     *
     * @param keyAndPassword
     *            the generated key and the new password
     * @return the ResponseEntity with status 200 (OK) if the password has been
     *         reset, or status 400 (Bad Request) or 500 (Internal Server Error)
     *         if the password could not be reset
     */
    @PostMapping(path = "/account/reset_password/finish", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            return new ResponseEntity<>("Incorrect password", HttpStatus.BAD_REQUEST);
        }
        return userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
            .map(user -> new ResponseEntity<String>(HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) && password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH && password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}
