\set ON_ERROR_STOP on
-- Query movements around the hospital in Inform-db

CREATE TEMPORARY VIEW DEMO_FACT_AGG AS
SELECT
pdf.fact_id,
pdf.attribute_id,
attrfact.description as attrfact,
string_agg(attrprop.description, '|') as attrprop,
string_agg(pdp.value_as_string, '|') as value
FROM patient_demographic_fact pdf
LEFT JOIN patient_demographic_property pdp ON pdp.fact = pdf.fact_id
LEFT JOIN attribute attrfact ON attrfact.attribute_id = pdf.attribute_id
LEFT JOIN attribute attrprop ON attrprop.attribute_id = pdp.attribute
group by pdf.fact_id, attrfact.description
order by fact_id, attribute_id
;

SELECT
mrn.mrn,
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
LEFT JOIN patient_demographic_fact pdf ON pdf.encounter = enc.encounter
LEFT JOIN DEMO_FACT_AGG dfa ON pdf.fact_id = dfa.fact_id
--WHERE mrn.mrn = '21011035'
ORDER BY mrn, visit_id, attribute
;

