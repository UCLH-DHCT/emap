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

> **Note**
> On the GAEs, first download and install [miniconda](https://docs.conda.io/en/latest/miniconda.html)

For example,

```bash
git clone https://github.com/inform-health-informatics/emap-setup.git &&\
cd emap-setup &&\
pip install -e . -r requirements.txt  &&\
cp global-configuration-EXAMPLE.yaml ../global-configuration.yaml
```

you may want to create and activate a virtual environment first with:

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

Clone all the repositories for the specified branches:
```bash
emap setup --init --branch test_branch
```

> **_NOTE:_**  If a branch has not been specified the runner defaults to the _develop_ branch.

Run a docker `ps` command:
```bash
emap docker ps
```
