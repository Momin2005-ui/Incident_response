package com.cloud_forensic_system.CLS.Repository;

import com.cloud_forensic_system.CLS.Model.Users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginRepo extends JpaRepository<Users,Long> {
     Optional<Users> findByName(String name);
}
