package uk.ac.ucl.rits.inform;

import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonRepository;

@SpringBootApplication
public class DBTester {
    @Autowired
    private PersonRepository repo;
    private final static Logger logger = LoggerFactory.getLogger(DBTester.class);

    public String savePerson() {
        Person psav = repo.save(new Person(42, java.sql.Timestamp.from(null)));
        return String.format("Hello world! %s", psav.toString());
    }

    public Person findOrAddPerson() {
        Optional<Person> pers = repo.findById(42);
        if (pers.isPresent()) {
            Person pgot = pers.get();
            System.out.println(pgot.toString());
            return pgot;

        } else {
            Person pnew = repo.save(new Person(42, java.sql.Timestamp.from(null)));
            System.out.println(pnew.toString());
            return pnew;
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ApplicationContext applicationContext = SpringApplication.run(DBTester.class, args);
        System.out.println("BEGIN: ");
        
        String[] foo = applicationContext.getBeanDefinitionNames();
        Arrays.sort(foo);
        for (String name : foo) {
            System.out.println(name);
        }
    }

}
