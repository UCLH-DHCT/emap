-- Add mrn

INSERT INTO public.mrn (mrn_id, mrn, nhs_number, source_system, stored_from) VALUES
    (1001, '40800000', '9999999999', 'EPIC', '2010-09-01 11:04:04.794000'),
    (1002, '60600000', '1111111111', 'caboodle', '2010-09-03 10:05:04.794000'),
    (1003, '30700000', null, 'EPIC', '2010-09-10 16:01:05.371000'),
    (1004, null, '1111111111', 'another', '2010-09-10 17:02:08.000000');

-- Add mrn_to_live

INSERT INTO public.mrn_to_live (mrn_to_live_id, stored_from, stored_until, valid_from, valid_until, live_mrn_id, mrn_id) VALUES
    (2001, '2010-09-01 11:04:04.794000', null, '2010-09-01 11:04:04.794000', null, 1001, 1001),
    (2002, '2010-09-03 10:05:04.794000', null, '2010-09-03 11:04:04.794000', null, 1003, 1002),
    (2003, '2010-09-10 16:01:05.371000', null, '2010-09-03 13:06:05.794000', null, 1003, 1003),
    (2004, '2010-09-10 17:02:08.000000', null, '2010-09-05 14:05:04.794000', null, 1004, 1004);


-- Add to core_demographic
INSERT INTO public.core_demographic (
    core_demographic_id, stored_from, valid_from, alive, date_of_birth, date_of_death,
    datetime_of_birth, datetime_of_death, firstname, home_postcode, lastname, middlename, mrn_id, sex
    ) VALUES
        (3001, '2012-09-17 13:25:00.650000', '2010-09-14 15:27:00.933000', true, '1970-05-05', null,
         '1970-05-05 04:10:00.000000', null, 'Sesame', 'W12 0HG', 'Snaps', null, 1003, 'F'),
        (3002, '2010-09-01 11:04:04.794000', '2011-02-11 10:00:52.000000', true, '1980-01-01', null,
         '1980-01-01 00:00:00.000000', null, 'lemon', null, 'zest', 'middle', 1001, 'F'),
        (3003, '2010-06-17 13:25:00.650000', '2010-02-16 10:00:52.000000', false, '1972-06-14', '2010-05-05',
         '1972-06-14 13:00:00.000000', '2010-05-05 00:00:00.000000', 'Terry', 'T5 1TT', 'AGBTESTD', null, 1002, 'M');