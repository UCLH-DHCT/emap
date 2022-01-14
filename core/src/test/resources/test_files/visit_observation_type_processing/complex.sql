-- Add mrn
INSERT INTO public.mrn (mrn_id, mrn, nhs_number, source_system, stored_from)
VALUES (1001, '40800000', '9999999999', 'EPIC', '2010-09-01 11:04:04');

-- Add hospital visit
INSERT INTO public.hospital_visit (
    hospital_visit_id, stored_from, valid_from, admission_time, arrival_method, discharge_destination, discharge_disposition,
    discharge_time, encounter, patient_class, presentation_time, source_system, mrn_id)
VALUES (2001, '2012-09-17 13:25:00', '2010-09-14 15:27:00', null, 'Public trans', null, null,
       null, '123412341234', 'INPATIENT', '2012-09-17 13:25:00', 'EPIC', 1001);

-- Add visit observation types
INSERT INTO public.visit_observation_type (visit_observation_type_id, id_in_application, interface_id, source_observation_type, valid_from, stored_from)
VALUES (3001, '38577', '', 'flowsheet', '2021-02-01 00:00:00.00', '2021-02-01 00:00:00.00'),
       (3002, '', '38577', 'flowsheet', '2021-02-02 00:00:00.00', '2021-02-02 00:00:00.00');

INSERT INTO public.visit_observation (visit_observation_id, hospital_visit_id, visit_observation_type_id, observation_datetime, source_system, valid_from, stored_from)
VALUES (4001, 2001, 3001, '2021-02-01 00:00:00.00', 'caboodle', '2021-02-01 00:00:00.00', '2021-02-01 00:00:00.00'),
       (4002, 2001, 3002, '2021-02-02 00:00:00.00', 'EPIC', '2021-02-02 00:00:00.00', '2021-02-02 00:00:00.00');