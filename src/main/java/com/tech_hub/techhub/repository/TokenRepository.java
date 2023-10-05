package com.tech_hub.techhub.repository;

import com.tech_hub.techhub.entity.PasswordResetToken;
import com.tech_hub.techhub.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<PasswordResetToken,Long> {

    PasswordResetToken findByToken(String token);
    PasswordResetToken findByUser(UserEntity user);

}
