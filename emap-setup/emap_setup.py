import os
import sys
from repo_setup import RepoSetup
from repo_setup import ReadConfig


def create_repostories(main_dir, repos, git_dir):
    """
    Create repositories
    :param main_dir: Directory to work in
    :param repos: dictionary items describing repos
    :param git_dir: path of main git repositories
    """
    r_setup = RepoSetup(main_dir, repos, git_dir)
    r_setup.clone_necessary_repos()


def main(args):
    main_dir = os.getcwd()
    filename = os.path.join(main_dir, '..', 'emap-setup', 'global-configuration.yaml')
    config_file = ReadConfig(filename)
    create_repostories(main_dir, config_file.get_repo_info(), config_file.get_git_dir())
    print("All done")


if __name__ == '__main__':
    main(sys.argv)
