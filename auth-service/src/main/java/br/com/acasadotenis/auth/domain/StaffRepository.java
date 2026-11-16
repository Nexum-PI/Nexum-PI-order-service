package br.com.acasadotenis.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<StaffMember, Long> {
    Optional<StaffMember> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
