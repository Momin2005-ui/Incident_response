package com.cloud_forensic_system.CLS.LogAnalysis.Repository;

import com.cloud_forensic_system.CLS.LogAnalysis.Model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Template findByPattern(String pattern);
}
