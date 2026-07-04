package com.onticoworkshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.User;

public interface UserRepository extends JpaRepository<User, String> {

  Optional<User> findByUsername(String username);
}
