-- Add visit observation types
INSERT INTO public.mrn (mrn_id, mrn, nhs_number, source_system, stored_from)
VALUES (1001, '40800000', '9999999999', 'EPIC', '2010-09-01 11:04:04'),
       (1002, '60600000', '1111111111', 'caboodle', '2010-09-03 10:05:04'),
       (1003, '30700000', null, 'EPIC', '2010-09-10 16:01:05'),
       (1004, null, '222222222', 'another', '2010-09-10 17:02:08');