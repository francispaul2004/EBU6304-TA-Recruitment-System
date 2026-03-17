package edu.bupt.ta.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID> {
    @JsonIgnore
    ID getId();
}
