Repos to include:
- git@github.com:inform-health-informatics/Inform-DB.git
- git@github.com:inform-health-informatics/Emap-Interchange.git
- git@github.com:inform-health-informatics/Emap-Core.git
- git@github.com:inform-health-informatics/emap-hl7-processor.git
- git@github.com:inform-health-informatics/emap-setup.git
- git@github.com:inform-health-informatics/emap_documentation.git (public docs)

Exclude:
- git@github.com:inform-health-informatics/hoover.git (temporarily exclude)
- git@github.com:inform-health-informatics/internal_emap_docs.git (is internal)

Any others I've forgotten?

## Tasks/Requirements

### Phase 1a (essential tasks)

(Eg. code will fail to compile/run/deploy if we don't do them, or they're so integral to the merge it would almost be harder *not* to do them in the first phase)

- Merge in `Inform-DB`
- Merge in `Emap-Interchange`
- Merge in `Emap-Core`
- Merge in `emap-hl7-processor`
- Preserve history - ensure git log and git blame give expected results
- Make names match (and follow the same capitalisation+punctuation) between:
	- docker service names
	- module directories
	- maven modules
	- output jar
- Merge Emap config files
- Merge/simplify docker-compose files
- Fix/simplify CI scripts, convert Circle to GHA
- Fix/simplify glowroot config
- Merge `inform-checker.xml` files (`hl7-reader` may need its own)
- Rename project/JAR name in poms (`finalName` and `artifactId` tags)

### Phase 1b (delayable tasks, but will attempt as phase 1 stretch goals)

- Bring in all tags, prefix them to avoid clashes and signify oldness
- Rename package names `rits.inform` -> `arc.emap`
- Merge in `emap_documentation` to docs subfolder

### Phase 2 (delayed tasks)

(Within scope of the project but things will work well enough without them)

- Make sure auto doc generation for former `emap_documentation` repo is working
- Merge in `emap-setup`



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
