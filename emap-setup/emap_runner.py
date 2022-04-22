import os
import argparse


from emap_setup.read_config import ConfigFile
from emap_setup.parser import Parser
from emap_setup.setup.config_dir_setup import create_or_update_config_dir
from emap_setup.setup.repo_setup import RepoSetup
from emap_setup.docker.docker_runner import DockerRunner

main_dir = os.path.dirname(os.path.abspath(__file__))


def create_parser() -> Parser:
    """Create a custom argument parser"""

    parser = Parser(description='Setup, update and run an instance of EMAP')

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
    docker_parser.add_argument(
        'docker_compose_args',
        help='Subcommands to pass to docker-compose. E,g, up, ps, down',
        nargs='+'
    )
    docker_parser.add_argument(
        '-fake', '--fake-epic',
        help='Include services for fake clarity and caboodle servers',
        default=False,
        action='store_true'
    )

    return parser


def setup(args:        argparse.Namespace,
          config_file: ConfigFile
          ) -> None:
    """Run the setup"""

    repo_setup = RepoSetup(main_dir=main_dir,
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

    create_or_update_config_dir(main_dir=main_dir, config_file=config_file)

    return None


def docker(args:        argparse.Namespace,
           config_file: ConfigFile) -> None:
    """Run a docker instance"""

    runner = DockerRunner(main_dir=main_dir,
                          use_fake_epic=args.fake_epic,
                          config=config_file.config)

    runner.inject_ports()
    paths = runner.docker_compose_paths

    if not all(os.path.exists(path) for path in paths):
        _paths_str = "\n".join(paths)
        exit(f'Cannot run docker-compose {args.docker_compose_args}. '
             f'At least one path did not exist:\n {_paths_str} ')

    os.system('docker-compose -f '+' -f '.join(paths)
              + f' -p {config_file.emap_project_name} '
              + ' '.join(args.docker_compose_args))

    return None


def main():
    parser = create_parser()
    args = parser.parse_args()

    if not os.path.exists(args.filename):
        exit(f'Configuration file *{args.filename}* not found. Exiting')

    config_file = ConfigFile(filename=args.filename)

    if args.subcommand == 'setup':
        setup(args, config_file)

    elif args.subcommand == 'docker':
        docker(args, config_file)

    else:
        exit('Run --help for options')

    print("All done")


if __name__ == '__main__':
    main()
