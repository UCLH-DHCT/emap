\set ON_ERROR_STOP on
-- Query movements around the hospital in Inform-db

CREATE TEMPORARY VIEW DEMO_FACT_AGG AS
SELECT
enc.encounter,
pdf.fact_id,
pdf.attribute_id,
attrfact.short_name as attrfact,
string_agg(attrprop.description, '|') as attrprop,
string_agg(pdp.value_as_string, '|') as value
FROM patient_demographic_fact pdf
LEFT JOIN patient_demographic_property pdp ON pdp.fact = pdf.fact_id
LEFT JOIN attribute attrfact ON attrfact.attribute_id = pdf.attribute_id
LEFT JOIN attribute attrprop ON attrprop.attribute_id = pdp.attribute
LEFT JOIN encounter enc ON pdf.encounter = enc.encounter
WHERE attrfact.short_name = 'NAMING'
group by pdf.fact_id, attrfact.short_name, enc.encounter
order by fact_id, attribute_id
;

-- transfers
SELECT
mrn.mrn,
dfa.attrprop,
dfa.value,
vf.visit_id,
vf.attribute_id as visit_type,
attrkey.description as attribute,
vp.value_as_string,
vp.value_as_datetime,
attrval.description as value_as_attribute,
enc.encounter
FROM visit_property vp
LEFT JOIN attribute attrkey ON attrkey.attribute_id = vp.attribute
LEFT JOIN attribute attrval ON attrval.attribute_id = vp.value_as_attribute
INNER JOIN visit_fact vf ON vf.visit_id = vp.visit
INNER JOIN encounter enc ON enc.encounter = vf.encounter
INNER JOIN mrn ON mrn.mrn = enc.mrn
LEFT JOIN DEMO_FACT_AGG dfa ON enc.encounter = dfa.encounter
--WHERE mrn.mrn = '21011035'
ORDER BY mrn, visit_id, attribute
;


-- demographics
SELECT
enc.encounter,
attrkey.short_name,
pdp.value_as_datetime,
attrval.short_name
from patient_demographic_property pdp
inner join patient_demographic_fact pdf on pdp.fact = pdf.fact_id
inner join encounter enc on enc.encounter = pdf.encounter
inner join attribute attrkey on pdp.attribute = attrkey.attribute_id
left join attribute attrval on pdp.value_as_attribute = attrval.attribute_id
where attrkey.short_name in ('DOB', 'SEX')
order by encounter, attrkey.short_name
;

-- locations + times
CREATE TEMPORARY VIEW VISIT_TIMES AS
SELECT
enc.encounter,
attrkey.short_name,
arrivaltime.value_as_datetime as arrival_time,
dischtime.value_as_datetime as discharge_time,
vp.value_as_string as location
from visit_property vp
inner join visit_fact vf on vp.visit = vf.visit_id
inner join encounter enc on enc.encounter = vf.encounter
inner join attribute attrkey on vp.attribute = attrkey.attribute_id
left join visit_property arrivaltime on arrivaltime.visit = vf.visit_id AND arrivaltime.attribute = (select attribute_id from attribute where short_name = 'ARRIVAL_TIME')
left join visit_property dischtime on dischtime.visit = vf.visit_id AND dischtime.attribute = (select attribute_id from attribute where short_name = 'DISCH_TIME')
where attrkey.short_name in ('LOCATION')
order by encounter, arrival_time
;

-- all locations and times
SELECT * FROM VISIT_TIMES
order by arrival_time
;

-- locations and times for people currently in the hospital
SELECT * FROM VISIT_TIMES
WHERE discharge_time is null
order by arrival_time
;

-- locations and times for people currently in the hospital, organised
-- by location (can check whether two patients appear to be in the same bed)
SELECT * FROM VISIT_TIMES
WHERE discharge_time is null
order by location
;

SELECT location,
count(*),
min(NOW() - arrival_time) as min_since_arrival,
avg(NOW() - arrival_time) as avg_since_arrival,
max(NOW() - arrival_time) as max_since_arrival
FROM VISIT_TIMES
WHERE discharge_time is null
AND location like 'ED%'
group by location
order by location
;
