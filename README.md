# Emap
A monorepo for all core Emap functions

# Basic layout
```
EMAP [your root emap directory]
├── config [config files passed to docker containers, not in any repo]
├── hoover [different repo]
├── emap [this repo]
│   ├── emap-star         [ formerly Inform-DB repo ]
│   ├── emap-interchange  [ formerly Emap-Interchange repo ]
│   ├── hl7-reader        [ formerly emap-hl7-processor repo ]
│   ├── core              [ formerly Emap-Core repo ]
│   ├── [etc.]
```

# Using IntelliJ with emap
How to [configure IntelliJ](docs/intellij.md) to build emap and run tests.

# Monorepo migration
How were [old repos migrated into this repo?](docs/migration.md)
