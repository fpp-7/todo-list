package br.com.project.to_do.model;


import jakarta.persistence.*;
import lombok.*;


@Data
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String description;
    private boolean done;

}
