import os
import argparse

from emap_setup.code_setup.read_config import ReadConfig
from emap_setup.code_setup.config_dir_setup import ConfigDirSetup
from emap_setup.code_setup.repo_setup import RepoSetup


def create_or_update_repositories(main_dir: str,
                                  config_file: ReadConfig,
                                  initial: bool) -> None:
    """
    Create repositories
    :param main_dir: Directory to work in
    :param config_file: ReadConfig class
    :param initial: bool indicating whether this is an initial setup or
    an update
    """
    r_setup = RepoSetup(main_dir, config_file.get_git_dir(),
                        config_file.get_repo_info())
    if initial:
        r_setup.clone_necessary_repos()
    else:
        r_setup.update_necessary_repositories()


def create_or_update_config_dir(main_dir: str,
                                config_file: ReadConfig) -> None:
    """
    Create the config dir populating and copying any envs files
    from the repositories present
    :param main_dir: Directory to work in
    :param config_file: Name of the configuration file
    """
    cd_setup = ConfigDirSetup(main_dir, config_file)
    cd_setup.create_or_update_config_dir()


def define_arguments():
    parser = argparse.ArgumentParser(
        description='Setup, update and run an instance of emap'
    )
    group = parser.add_argument_group()
    group.add_argument('setup',
                       default=None,
                       help='Initialise/update repository directories',
                       nargs='?')
    group.add_argument('-f', '--filename',
                       help='Path to the .yaml file containing the global '
                            'configuration',
                       default='global-configuration.yaml')
    group1 = group.add_mutually_exclusive_group()
    group1.add_argument('-i', '--init',
                        help='clone repositories and create config dir',
                        default=False,
                        action='store_true')
    group1.add_argument('-u', '--update',
                        help='update repositories and config files',
                        default=False,
                        action='store_true')
    # place holder for second group relating to docker
    group2 = parser.add_argument_group()
    group2.add_argument('docker',
                        default=None,
                        help='something',
                        nargs='?')
    group3 = group2.add_mutually_exclusive_group()
    group3.add_argument('-r', '--run',
                        help='run ',
                        default=False,
                        action='store_true')
    group3.add_argument('-up', '--up',
                        help='up',
                        default=False,
                        action='store_true')
    return parser


def main():
    parser = define_arguments()
    args = parser.parse_args()

    if os.path.exists(args.filename):
        config_file = ReadConfig(args.filename)
    else:
        print(f'Configuration file *{args.filename}* not found')
        exit(1)
    print(args.init)

    print(args)

    if args.setup:
        create_or_update_repositories(os.getcwd(), config_file, args.init)
        create_or_update_config_dir(os.getcwd(), config_file)

    if args.docker:
        raise NotImplementedError

    print("All done")


if __name__ == '__main__':
    main()
