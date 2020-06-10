import os
import sys
from repo_setup import RepoSetup
from repo_setup import ReadConfig


def read_config_vars(filename):
    """
    Read the global-configuration file and extract daa for relevant sections
    :param filename: full path to global-configuration.yaml
    :return:
    """
    config_file = ReadConfig(filename)
    repos = config_file.populate_repo_info()
    return repos


def create_repostories(main_dir, repos):
    """
    Create repositories
    :param main_dir: Directory
    :param repos: list of dictionary items describing repos
    :return:
    """
    r_setup = RepoSetup(main_dir, repos)
    r_setup.clone_necessary_repos()


def main(args):
    main_dir = os.getcwd()
    filename = os.path.join(main_dir, '..', 'emap-setup', 'global-configuration.yaml')
    repos = read_config_vars(filename)
    create_repostories(main_dir, repos)
    print("All done")


if __name__ == '__main__':
    main(sys.argv)
