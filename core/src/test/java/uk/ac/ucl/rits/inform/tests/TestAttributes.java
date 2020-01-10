package uk.ac.ucl.rits.inform.tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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
@RunWith(SpringRunner.class)
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
    @Before
    public void setUp() {
        enumShortNames = new TreeSet<>();
        for (AttributeKeyMap attr: AttributeKeyMap.values()) {
            enumShortNames.add(attr.getShortname());
        }
    }

    /**
     * Ensure that loading the vocab twice is harmless. (Should already have been loaded on app load).
     */
    @Before
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
}
