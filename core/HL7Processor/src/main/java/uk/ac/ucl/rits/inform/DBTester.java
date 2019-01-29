package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ucl.rits.inform.informdb.Person;
import uk.ac.ucl.rits.inform.informdb.PersonRepository;

@Component
public class DBTester {
    @Autowired
    private PersonRepository repo;
    private final static Logger logger = LoggerFactory.getLogger(DBTester.class);

    public DBTester() {
    }

    public String savePerson() {
        Person psav = repo.save(new Person(42, Timestamp.from(Instant.now())));
        return String.format("Hello world! %s", psav.toString());
    }

    public Person findOrAddPerson() {
        Optional<Person> pers = repo.findById(42);
        if (pers.isPresent()) {
            Person pgot = pers.get();
            System.out.println(pgot.toString());
            return pgot;

        } else {
            Person pnew = repo.save(new Person(42, Timestamp.from(Instant.now())));
            System.out.println(pnew.toString());
            return pnew;
        }
    }

}
