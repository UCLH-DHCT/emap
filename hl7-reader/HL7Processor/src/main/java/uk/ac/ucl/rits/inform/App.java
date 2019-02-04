package uk.ac.ucl.rits.inform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import uk.ac.ucl.rits.inform.informdb.Encounter;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
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
