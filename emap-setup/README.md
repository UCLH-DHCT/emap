# emap-setup

Code to initialise, update and run EMAP. Includes ability to:
1. Clone repos
2. Set up configuration
3. Run pipeline
4. Update code (`git pull`)

## Usage
1. Create your working directory 
2. Clone emap-setup into that directory
3. Copy global-configuration-EXAMPLE.yaml as global-configuration.yaml and adjust for your own requirements

For example,

```bash
git clone https://github.com/inform-health-informatics/emap-setup.git &&\
cd emap-setup &&\
cp global-configuration-EXAMPLE.yaml global-configuration.yaml &&\
python emap_runner.py setup --init
```

### Command line options
```bash
python emap_runner.py --help
```
 
## Notes
1. NEED a docker container to allow this to run on gae
