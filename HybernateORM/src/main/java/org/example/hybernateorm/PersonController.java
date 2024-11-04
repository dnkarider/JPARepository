package org.example.hybernateorm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {
    private final PersonRepository personRepository;

    @Autowired
    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping("/persons/by-city")
    //        При запуске приложения создается пустая табличка -> получите отбивку - в городе никто не живет
    //        Для инсертов использовал скрипт из папки /scripts
    public String fetchProduct(@RequestParam String city) {
        return personRepository.getPersonsByCity(city);
    }
}
