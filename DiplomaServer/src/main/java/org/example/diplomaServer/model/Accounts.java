package org.example.diplomaServer.model;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Accounts {
    @Id
    private String id;
    @Column(nullable = false)
    private String login;
    @Column(nullable = false)
    private String password;

}
