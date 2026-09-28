package com.cloud_forensic_system.CLS.Service;

import com.cloud_forensic_system.CLS.Model.Users;
import com.cloud_forensic_system.CLS.Repository.LoginRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final LoginRepo loginRepo;

    public void addUser(Users user) {

        Users saved = loginRepo.save(user);
        log.info("REGISTER_SUCCESS userId={} email={}", saved.getUserId(), saved.getPassword());
    }

    public boolean checkUser(String username,String password) {
        Optional<Users> user = loginRepo.findByName(username);
        if (user.isPresent()) {
            System.out.println(user);
            Users us =user.get();
            if(us.getPassword().equals(password)){
                log.info("LOGIN_SUCCESS username={} userId={}", username, user.get().getUserId());
                return true;
            }
            else{
                log.info("LOGIN_FAILED_PASSWORD_MISMATCH username={} userId={}", username, user.get().getUserId());
                return false;
            }
        } else {
            log.warn("LOGIN_FAILED_USER_NOT_FOUND username={}", username);
            return false;
        }
    }

    public boolean checkIsUserRegistered(String username){
        Optional<Users> user = loginRepo.findByName(username);
        if (user.isPresent()) {
            log.info("USER_ALREADY_REGISTERD username={} userId={}", username, user.get().getUserId());
            return true;
        } else {
            log.warn("SUCCESSFULLY_REGISTERED username={}", username);
            return false;
        }
    }
}