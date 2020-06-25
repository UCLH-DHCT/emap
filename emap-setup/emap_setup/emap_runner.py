import os
import sys
import argparse

from emap_setup.code_setup import ReadConfig
from emap_setup.code_setup import ConfigDirSetup
from emap_setup.code_setup import RepoSetup

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


def define_arguments():
    parser = argparse.ArgumentParser(description='Set up or update emap installation.')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('-init', help='clone repositories and create config dir', required=False)
    group.add_argument('-update', help='update repositories and config files', required=False)
    return parser


def main(args):
    # get arguments
    opts = [opt for opt in sys.argv[1:] if opt.startswith("-")]

 #   if len(opts) == 0 or len(sys.argv) > 2:
 #       usage()

    #
    parser = define_arguments()
    args = parser.parse_args()
    print(args)
    print(args.init)
    # set up main variables
    main_dir = os.getcwd()
    filename = os.path.join(main_dir, '..', 'emap-setup', 'global-configuration.yaml')
    if os.path.exists(filename):
        config_file = ReadConfig(filename)
    else:
        # TODO put in errr mess
        exit(1)
    print(args.init)

    # run chosen action
    if args[0] == 'init':
        create_repostories(main_dir, config_file.get_repo_info(), config_file.get_git_dir())
        create_or_update_config_dir(main_dir, config_file)
    elif opts[0] == '-tests':
        create_repostories(main_dir, config_file.get_repo_info(), config_file.get_git_dir())
    print("All done")


if __name__ == '__main__':
    main(sys.argv[1:])
