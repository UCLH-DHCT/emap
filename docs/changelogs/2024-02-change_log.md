# EMAP Release

**Date: 2024-02-14 Changes in this release**

---

### Changes/fixes

- We have added a healthcheck endpoint for the hl7-reader, so that users can check the status of the hl7-reader without
  needing to access the server directly.
    - The current live version can be found at: `http://<GAE hosting EMAP>:8080/actuator/health`
    - Please contact the developer team to find out the GAE hostname.
- All services now run using Java 17 (LTS)
- Open source release using mono-repo
- Improved testing to ensure database consistency

### Tables changed

Department specialities can change over time, we've updated the database to reflect this.
The following tables have been updated:

| Table           | Attributes added | Attributes removed |
|:----------------|:-----------------|:-------------------|
| DepartmentState | speciality       | -                  |
| Department      | -                | speciality         


```mermaid
erDiagram
bed {
    varchar(255) hl7string
    bigint room_id
    bigint bed_id
}
department {
    varchar(255) hl7string
    bigint internal_id
    varchar(255) name
    bigint department_id
}
department_state {
  timestamp-with-time-zone stored_from
  timestamp-with-time-zone valid_from
  timestamp-with-time-zone stored_until
  timestamp-with-time-zone valid_until
  varchar(255) speciality
  varchar(255) status
  bigint department_id
  bigint department_state_id
}
location {
  varchar(255) location_string
  bigint bed_id
  bigint department_id
  bigint room_id
  bigint location_id
}
room {
  varchar(255) hl7string
  varchar(255) name
  bigint department_id
  bigint room_id
}

department  ||--|{  department_state: department_id
location |{--o| department: department_id
location |{--o| room: room_id
location |{--o| bed: bed_id
department  ||--o{  room: room_id 
room  ||--o{  bed: bed_id
```

---
<!--
## Data sources

### Repository Versions

| Repository            | Version |
| :-                    | :-:     |
|Hl7-processor          | 2.7     |
|Emap_interchange       | 2.7     |
|Emap-Core              | 2.7     |
|Inform-DB              | 2.7     |
|Hoover                 | 2.7     |
>
