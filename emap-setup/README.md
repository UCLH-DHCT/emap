# emap-setup

Code to allow users to run a single script that will
1. clone repos
2. set up configuration
3. run pipeline (Coming soon)
4. update code (Coming soon)
5. any other ideas

## Usage
1. Create your working directory 
2. Clone emap-setup into that directory
3. Copy global-configurations-EXAMPLE.yaml as global-configurations.yaml and adjust for your own requirements

For initial setup run ./emap-setup/emap-setup.py -init


### Command line options
 - -init  clones the required repositories and sets up and populates  the config directory 
 
 
## Notes
1. NEED a docker container to allow this to run on gae
