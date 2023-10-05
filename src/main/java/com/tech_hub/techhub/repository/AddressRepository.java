package com.tech_hub.techhub.repository;

import com.tech_hub.techhub.entity.Address;
import com.tech_hub.techhub.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address>findByUser(UserEntity user);
}
