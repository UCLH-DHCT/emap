\set ON_ERROR_STOP on

create TEMPORARY view bed_visits AS
select
enc.mrn
,vf.encounter
,vp.visit
,vp.stored_from
,vp.value_as_string
,adm.value_as_datetime as adm_time
,dis.value_as_datetime as disch_time
from visit_property vp
left join visit_property adm on adm.visit = vp.visit and adm.attribute = (select attribute_id from attribute where short_name = 'ARRIVAL_TIME')
left join visit_property dis on dis.visit = vp.visit and dis.attribute = (select attribute_id from attribute where short_name = 'DISCH_TIME')
inner join visit_fact vf on vp.visit = vf.visit_id
inner join encounter enc on vf.encounter = enc.encounter
where
vp.valid_until is null
and vp.attribute = (select attribute_id from attribute where short_name = 'LOCATION')
order by vf.encounter,vp.visit,vp.attribute
;

-- probably want to perform these queries in HQL and add them to junit tests eventually

-- properties with improper validity dates
select * from visit_property where valid_from is null or (valid_until is not null and valid_from > valid_until);

-- bed visits with discharge before admit, or null admit
select * from bed_visits where adm_time is null or (disch_time is not null and adm_time > disch_time);

