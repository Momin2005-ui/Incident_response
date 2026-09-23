package com.cloud_forensic_system.CLS.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController

public class Login {

    public ResponseEntity<?> login(@RequestBody Map<String,String> query){
         String username = query.get("name");
         String password =query.get("password");

         return null ;
    }
}
