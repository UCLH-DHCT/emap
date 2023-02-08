Repos to include:
- git@github.com:inform-health-informatics/Inform-DB.git
- git@github.com:inform-health-informatics/Emap-Interchange.git
- git@github.com:inform-health-informatics/Emap-Core.git
- git@github.com:inform-health-informatics/emap-hl7-processor.git

Exclude for now:
- git@github.com:inform-health-informatics/hoover.git

What about git@github.com:inform-health-informatics/emap-setup.git? Any others I've forgotten?


## Tasks/Requirements
- Preserve history - ensure git log and git blame give expected results
- Bring in all tags, prefix them to avoid clashes
- Make names match (and follow the same capitalisation+punctuation) between:
	- docker service names
	- module directories
	- maven modules
	- output jar
- Merge Emap config
- Merge/simplify docker-compose files
- Fix/simplify CI scripts, convert Circle to GHA
- Fix/simplify glowroot config
- Rename project/JAR name in poms (`finalName` and `artifactId` tags)
- Rename package names `rits.inform` -> `arc.emap`
- Bring in user documentation

## Limitations

Old PRs/issues won't get migrated

## Naming

New, merged repo will be called `emap`  and live under the new organisation

The new name column will be used for ALL purposes; that is, the docker service name, the subdirectory, the maven project name, and the output jar name.

| current repo name | NEW name | current docker service name  | current maven project name | current jar name  |
| --- | --- | --- | --- | --- |
| Inform-DB        | \[emap-star/\] emap-star | n/a | inform-db | `inform-db*.jar` |
| Inform-DB     |   \[emap-star/\] emap-star-annotations |  n/a  |  inform-annotations | `inform-annotations*.jar`  |
| Emap-Interchange | emap-interchange | n/a | emap-interchange | `emap-interchange*.jar` |
| Emap-Core       | core | emapstar  | EmapCore | `Emap-Core*.jar`  |
| emap-hl7-processor | hl7-reader | hl7source | HL7Processor | `HL7Processor*.jar` |
| hoover §     | hoover   | hoover  | hoover | `Hoover*.jar` |


§  not merging right now but can still rename things 
