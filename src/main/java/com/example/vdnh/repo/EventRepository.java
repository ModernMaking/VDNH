package com.example.vdnh.repo;

import com.example.vdnh.model.EventTable;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<EventTable,Long> {
}
