package org.example.hybernateorm;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Persons {

    private String phone_number;
    private String city_of_living;

    @EmbeddedId
    private BasicPersonInfo basicPersonInfo;
}
