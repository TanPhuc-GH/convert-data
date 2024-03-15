package com.tool.convertdata.storage.repositories;

import com.tool.convertdata.storage.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRepository extends JpaRepository<Phone,Double> {

    List<Phone> findAllByPhoneIn(List<Double> phones);
}
