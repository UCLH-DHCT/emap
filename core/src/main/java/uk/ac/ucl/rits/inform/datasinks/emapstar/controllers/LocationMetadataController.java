package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedPoolRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.BedStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.DepartmentRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.locations.RoomStateRepository;

/**
 * Adds or updates location metadata (department, room, bed pool, bed, and their history or states).
 * @author Stef Piatek
 */
@Component
public class LocationMetadataController {
    private static final Logger logger = LoggerFactory.getLogger(LocationMetadataController.class);

    private final DepartmentRepository departmentRepo;
    private final DepartmentAuditRepository departmentAuditRepo;
    private final RoomRepository roomRepo;
    private final RoomStateRepository roomStateRepo;
    private final BedPoolRepository bedPoolRepo;
    private final BedRepository bedRepo;
    private final BedStateRepository bedStateRepo;


    public LocationMetadataController(
            DepartmentRepository departmentRepo, DepartmentAuditRepository departmentAuditRepo,
            RoomRepository roomRepo, RoomStateRepository roomStateRepo,
            BedPoolRepository bedPoolRepo, BedRepository bedRepo, BedStateRepository bedStateRepo) {
        this.departmentRepo = departmentRepo;
        this.departmentAuditRepo = departmentAuditRepo;
        this.roomRepo = roomRepo;
        this.roomStateRepo = roomStateRepo;
        this.bedPoolRepo = bedPoolRepo;
        this.bedRepo = bedRepo;
        this.bedStateRepo = bedStateRepo;
    }

}
