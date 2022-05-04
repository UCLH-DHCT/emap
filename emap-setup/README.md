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
pip install . -r requirements.txt 
```

you may want to create and activate a virtual environment first with e.g.

```bash
conda create python=3.9 -n emap --yes &&\
conda activate emap
```

***
## Command line options

To see the top level options
```bash
emap --help
```

and for the setup subcommands
```bash
emap setup --help
```

### Examples

Clone all the repositories:
```bash
emap setup --init
```

Run a docker command:
```bash
emap docker ps
```


## Notes
1. NEED a docker container to allow this to run on gae
