# emap-setup

Code to initialise, update and run EMAP. Includes ability to:
1. Clone repos
2. Set up configuration
3. Run pipeline (live and validation runs)
4. Update code (`git pull`)


## Usage: GAE

`emap-setup` is already installed on GAE01 - activate the environment with
```bash
source /gae/emap_setup/venv/bin/activate
```
which may be helpful to place in your bash_profile, so it's activated on login.

The required `global-configuration.yaml` files exist in schema specific folders
e.g. `/gae/emap-<schema name>`.

## Usage: Local

1. Create your working directory 
2. Clone emap-setup into that directory
3. Copy global-configuration-EXAMPLE.yaml as global-configuration.yaml and adjust for your own requirements

For example, create and activate a virtual environment first with either:

<details><summary>Conda</summary>

```bash
conda create python=3.9 -n emap --yes &&\
conda activate emap
```

</details>
<details><summary>venv</summary>

```bash
mkdir -p ~/.local/venvs/emap &&\
python -m venv ~/.local/venvs/emap &&\
source ~/.local/venvs/emap/bin/activate
```

</details>

then clone and install 
```bash
git clone https://github.com/inform-health-informatics/emap-setup.git
cd emap-setup
pip install -e . -r requirements.txt
cp global-configuration-EXAMPLE.yaml ../global-configuration.yaml
```

***
## Command line options

To see the top level options:
```bash
emap --help
```

and for the setup subcommands e.g.:
```bash
emap setup --help
```

### Examples

Update all the repositories for the specified branches:
```bash
emap setup -u --branch test_branch
```

> **Note**
> If a branch has not been specified the runner defaults to the _develop_ branch.

Run a docker `ps` command:
```bash
emap docker ps
```

Run a validation
```bash
emap validation
```
