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
