package uk.ac.ucl.rits.inform;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ucl.rits.inform.informdb.*;

@Component
public class DBTester {
    @Autowired
    private PersonRepository personRepo;
    @Autowired
    private MrnRepository mrnRepo;
    private final static Logger logger = LoggerFactory.getLogger(DBTester.class);

    public DBTester() {
    }

    public String savePerson() {
        Person psav = personRepo.save(new Person(42, Timestamp.from(Instant.now())));
        return String.format("Hello world! %s", psav.toString());
    }

    public Mrn findOrAddMrn(Person person) {
        Mrn newMrn = new Mrn();
        newMrn.setStore_datetime(Timestamp.from(Instant.now()));
        newMrn.setPerson(person);
        newMrn = mrnRepo.save(newMrn);
        System.out.println(newMrn.toString());
        return newMrn;
    }
    
    public Person findOrAddPerson() {
        Optional<Person> pers = personRepo.findById(42);
        if (pers.isPresent()) {
            Person pgot = pers.get();
            System.out.println(pgot.toString());
            return pgot;

        } else {
            Person pnew = personRepo.save(new Person(42, Timestamp.from(Instant.now())));
            System.out.println(pnew.toString());
            return pnew;
        }
    }

}
