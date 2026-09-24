package com.cloud_forensic_system.CLS.Controller;

import com.cloud_forensic_system.CLS.Model.Users;
import com.cloud_forensic_system.CLS.Service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class Login {

    private final LoginService loginService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> query){
         String username = query.get("name");
         String password =query.get("password");

        Users user = new Users();
        user.setName(username);
        user.setPassword(password);

         try{
             loginService.addUser(user);
             return new ResponseEntity<>("User added", HttpStatus.CREATED);
         } catch (RuntimeException e) {
             throw new RuntimeException(e);
         }


    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> query){
        String username =query.get("username");
        String password =query.get("password");

        try{
           boolean bl = loginService.checkUser(username);
           if(bl){
               return new ResponseEntity<>("User found",HttpStatus.FOUND);
           }
           else{
               return new ResponseEntity<>("User not found",HttpStatus.NOT_FOUND);
           }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
