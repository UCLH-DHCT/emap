-- Add mrn

INSERT INTO public.mrn (mrn_id, mrn, nhs_number, source_system, stored_from) VALUES
    (1, '40800000', '9999999999', 'EPIC', '2020-09-01 11:04:04.794000'),
    (2, '60600000', '1111111111', 'caboodle', '2020-09-03 10:05:04.794000'),
    (3, '30700000', null, 'EPIC', '2020-09-10 16:01:05.371000'),
    (4, '60900000', '2222222222', 'another', '2020-09-10 17:02:08.000000');

-- Add mrn_to_live

INSERT INTO public.mrn_to_live (mrn_to_live_id, stored_from, stored_until, valid_from, valid_until, live_mrn_id, mrn_id) VALUES
    (1, '2020-09-01 11:04:04.794000', null, '2020-09-01 11:04:04.794000', null, 1, 1),
    (2, '2020-09-03 10:05:04.794000', null, '2020-09-03 11:04:04.794000', null, 3, 2),
    (3, '2020-09-10 16:01:05.371000', null, '2020-09-03 13:06:05.794000', null, 3, 3),
    (4, '2020-09-10 17:02:08.000000', null, '2020-09-05 14:05:04.794000', null, 4, 4);