package uk.ac.ucl.rits.inform.tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.ac.ucl.rits.inform.datasinks.emapstar.InformDbOperations;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.AttributeRepository;
import uk.ac.ucl.rits.inform.informdb.Attribute;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;

/**
 * Test that our attributes are consistent between the enum (AttributeKeyMap) and the
 * database (loaded from csv).
 * @author Jeremy Stein
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { uk.ac.ucl.rits.inform.datasinks.emapstar.App.class })
@ActiveProfiles("test")
public class TestAttributes {
    @Autowired
    private InformDbOperations dbOps;

    @Autowired
    private AttributeRepository attributeRepo;

    private SortedSet<String> enumShortNames;

    /**
     * Store short names from the attributes enum.
     */
    @BeforeEach
    public void setUp() {
        enumShortNames = new TreeSet<>();
        for (AttributeKeyMap attr: AttributeKeyMap.values()) {
            enumShortNames.add(attr.getShortname());
        }
    }

    /**
     * Ensure that loading the vocab twice is harmless. (Should already have been loaded on app load).
     */
    @BeforeEach
    public void loadVocabAgain() {
        dbOps.ensureVocabLoaded();
    }

    /**
     * Check there is at least one attribute, otherwise something clearly didn't load properly.
     */
    @Test
    public void testSomeKnown() {
        assertTrue("There should be some enum attributes", !enumShortNames.isEmpty());
    }

    /**
     * Check that all enum attributes are in the DB.
     * Check that all DB attributes are in the enum.
     * This ensures these two sources have been kept up to date with each other.
     */
    @Test
    public void testAllPresent() {
        Set<Attribute> inDb = attributeRepo.findAll();
        SortedSet<String> shortNamesInDb = new TreeSet<>(inDb.stream().map(attr -> attr.getShortName()).collect(Collectors.toSet()));

        Collection<Serializable> enumOnly = CollectionUtils.subtract(enumShortNames, shortNamesInDb);
        Collection<Serializable> dbOnly = CollectionUtils.subtract(shortNamesInDb, enumShortNames);
        assertEquals("All enum attributes should be in the DB, and vice versa. In enum only: " + enumOnly + "  In db only: " + dbOnly,
                shortNamesInDb, enumShortNames);
    }

    /**
     * Check that AttributeKeyMap enum values are deprecated iff there is a validUntil date.
     */
    @Test
    public void checkAttributeDeprecation() {
        Field[] declaredFields = AttributeKeyMap.class.getDeclaredFields();
        int numChecked = 0;
        for (Field field: declaredFields) {
            // exclude the non-enum constants like shortname
            if (field.isEnumConstant()) {
                numChecked++;
                Deprecated annotation = field.getAnnotation(Deprecated.class);
                boolean isDeprecated = annotation != null;
                AttributeKeyMap attrKM = AttributeKeyMap.valueOf(field.getName());
                Optional<Attribute> attr = attributeRepo.findByShortName(attrKM.getShortname());
                Attribute attribute = attr.get();
                boolean hasValidUntil = attribute.getValidUntil() != null;
                assertTrue(String.format("Attr %s has deprecation status %b but validUntil status %b",
                        attribute.getShortName(), isDeprecated, hasValidUntil), isDeprecated == hasValidUntil);
            }
        }
        assertEquals("Number of checks does not match number of enum values", attributeRepo.count(), numChecked);
    }
}
