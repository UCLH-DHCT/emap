package uk.ac.ucl.rits.inform;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import uk.ac.ucl.rits.inform.informdb.Encounter;
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
            long startTimeMillis = System.currentTimeMillis();
            for (int i = 0; i < 5e4; i++) {
                Encounter enc = dbt.addEncounter(new A01Wrap());
                if (i % 100 == 0) {
                    System.out.println("[" + i + "]" + enc.toString());
                }
            }
            long endcurrentTimeMillis = System.currentTimeMillis();
            System.out.println(String.format("done, took %.0f secs", (endcurrentTimeMillis - startTimeMillis)/1000.0));
        };
    }
}
