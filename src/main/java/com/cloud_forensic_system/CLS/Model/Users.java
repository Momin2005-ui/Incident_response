package com.cloud_forensic_system.CLS.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Users {

    @GeneratedValue
    @Id
    Long userId;
    String Name;
    String Password;

}
