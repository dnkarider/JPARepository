package org.example.hybernateorm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    @PersistenceContext
    EntityManager entityManager;

    public String getPersonsByCity(String city) {
        final String[] info = {""};
        var persons = entityManager.createNativeQuery("SELECT * FROM persons name WHERE city_of_living LIKE 'MOSCOW'").getResultList();
        persons.forEach(row -> {
            var rowArray = (Object[]) row;
            info[0] += "name: " + rowArray[1] + "; surname: " + rowArray[2];
        });
        if (info[0].equals("")) {
            info[0] = "В этом городе никто не живет";
        }
        return info[0];
    }
}
