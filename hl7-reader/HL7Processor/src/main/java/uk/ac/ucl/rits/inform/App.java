package uk.ac.ucl.rits.inform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import uk.ac.ucl.rits.inform.informdb.Mrn;
import uk.ac.ucl.rits.inform.informdb.Person;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        SpringApplication.run(App.class, args);
    }
    
    @Bean
    public CommandLineRunner mainLoop(DBTester dbt) {
        return (args) -> {
            System.out.println("hi");
            Person p = dbt.findOrAddPerson();
            System.out.println("Person: " + p.toString());
            Mrn mrn = dbt.findOrAddMrn(p);
            System.out.println("Related Mrn: " + mrn.toString());
        };
    }
}
