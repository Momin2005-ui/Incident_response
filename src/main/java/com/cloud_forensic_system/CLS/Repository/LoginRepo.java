package com.cloud_forensic_system.CLS.Repository;

import com.cloud_forensic_system.CLS.Model.Users;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRepo extends JpaRepository<Users,Long> {

}
