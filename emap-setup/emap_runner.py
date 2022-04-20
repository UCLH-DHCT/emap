import os
import argparse

from emap_setup.setup.read_config import ConfigFile
from emap_setup.setup.config_dir_setup import create_or_update_config_dir
from emap_setup.setup.repo_setup import RepoSetup


def create_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description='Setup, update and run an instance of EMAP'
    )

    parser.add_argument(
        '-f', '--filename',
        help='Path to the .yaml file containing the global configuration.\n'
             'Default: global-configuration.yaml',
        default='global-configuration.yaml'
    )

    subparsers = parser.add_subparsers(help='sub-command help',
                                       dest='subcommand')
    setup_parser = subparsers.add_parser(
        'setup',
        help='Initialise/update repository directories'
    )

    setup_type_group = setup_parser.add_mutually_exclusive_group()
    setup_type_group.add_argument(
        '-i', '--init',
        help='Clone repositories and create config dir',
        default=False,
        action='store_true'
    )
    setup_type_group.add_argument(
        '-u', '--update',
        help='Update repositories and config files',
        default=False,
        action='store_true'
    )
    setup_type_group.add_argument(
        '-c', '--clean',
        help='Clean the repositories and config files',
        default=False,
        action='store_true'
    )

    docker_parser = subparsers.add_parser(
        'docker',
        help='Run the docker instance'
    )

    docker_type_group = docker_parser.add_mutually_exclusive_group()
    docker_type_group.add_argument('-r', '--run',
                                   help='run ',
                                   default=False,
                                   action='store_true')
    docker_type_group.add_argument('-u', '--up',
                                   help='up',
                                   default=False,
                                   action='store_true')
    return parser


def setup(args: argparse.Namespace) -> None:
    """Run the setup"""

    if not os.path.exists(args.filename):
        exit(f'Configuration file *{args.filename}* not found. Exiting')

    config_file = ConfigFile(filename=args.filename)

    repo_setup = RepoSetup(main_dir=os.getcwd(),
                           git_dir=config_file.git_dir,
                           repos=config_file.repo_info)

    if args.init:
        repo_setup.clone()

    elif args.update:
        repo_setup.update()

    elif args.clean:
        repo_setup.clean()

    else:
        exit('Please run --help for options')

    create_or_update_config_dir(main_dir=os.getcwd(), config_file=config_file)

    return None


def docker(args: argparse.Namespace) -> None:
    """Run a docker instance"""


    return None


def main():
    parser = create_parser()
    args = parser.parse_args()

    if args.subcommand == 'setup':
        setup(args)

    if args.subcommand == 'docker':
        raise NotImplementedError

    print("All done")


if __name__ == '__main__':
    main()
