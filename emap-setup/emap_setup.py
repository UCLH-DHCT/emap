import os
import sys
from code_setup import RepoSetup
from code_setup import ReadConfig
from code_setup import ConfigDirSetup


def usage():
    print('Usage emap-setup.py args')
    print('where args is one of:\n')
    print('-init initial setup from scratch')
    print('more to follow')
    print('\n')
    exit()


def create_repostories(main_dir, repos, git_dir):
    """
    Create repositories
    :param main_dir: Directory to work in
    :param repos: dictionary items describing repos
    :param git_dir: path of main git repositories
    """
    r_setup = RepoSetup(main_dir, repos, git_dir)
    r_setup.clone_necessary_repos()


def create_or_update_config_dir(main_dir, config_file):
    """
    Create the config dir populating and copying any envs files from the repositories present
    :param main_dir: Directory to work in
    :param config_file: Name of the configuration file
    """
    cd_setup = ConfigDirSetup(main_dir, config_file)
    cd_setup.create_or_update_config_dir()


def main(args):
    # get arguments
    opts = [opt for opt in sys.argv[1:] if opt.startswith("-")]

    if len(opts) == 0 or len(sys.argv) > 2:
        usage()

    # set up main variables
    main_dir = os.getcwd()
    filename = os.path.join(main_dir, '..', 'emap-setup', 'global-configuration.yaml')
    config_file = ReadConfig(filename)

    # run chosen action
    if opts[0] == '-init':
#        create_repostories(main_dir, config_file.get_repo_info(), config_file.get_git_dir())
        create_or_update_config_dir(main_dir, config_file)
    elif opts[0] == '-test':
        create_repostories(main_dir, config_file.get_repo_info(), config_file.get_git_dir())
    print("All done")


if __name__ == '__main__':
    main(sys.argv)
