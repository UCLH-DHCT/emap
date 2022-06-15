import argparse

from pathlib import Path

from emap_runner.parser import Parser
from emap_runner.utils import TimeWindow
from emap_runner.global_config import GlobalConfiguration
from emap_runner.docker.docker_runner import DockerRunner
from emap_runner.validation.validation_runner import ValidationRunner
from emap_runner.utils import EMAPRunnerException


def create_parser() -> Parser:
    """Create a custom argument parser"""

    parser = Parser(description="Setup, update and run an instance of EMAP")

    parser.add_argument(
        "-f",
        "--filename",
        help="Path to the .yaml file containing the global configuration.\n"
        "Default: global-configuration.yaml",
        default="global-configuration.yaml",
    )

    subparsers = parser.add_subparsers(help="sub-command help", dest="subcommand")

    setup_parser = subparsers.add_parser(
        "setup", help="Initialise/update repository directories"
    )

    setup_type_group = setup_parser.add_mutually_exclusive_group()
    setup_type_group.add_argument(
        "-i",
        "--init",
        help="Clone repositories and create config dir",
        default=False,
        action="store_true",
    )
    setup_type_group.add_argument(
        "-u",
        "--update",
        help="Update repositories and config files",
        default=False,
        action="store_true",
    )
    setup_type_group.add_argument(
        "-c",
        "--clean",
        help="Clean the repositories and config files",
        default=False,
        action="store_true",
    )
    setup_type_group.add_argument(
        "-g",
        "--only_update_config_from_global",
        help="Only update the configuration",
        default=False,
        action="store_true",
    )

    setup_parser.add_argument(
        "-b",
        "--branch",
        help="Name of the branch to clone/update to. Overrides those defined "
        "in the global configuration. Falls back to develop if the branch "
        "does not exist",
        default=None,
        type=str,
    )

    docker_parser = subparsers.add_parser("docker", help="Run the docker instance")
    docker_parser.add_argument(
        "docker_compose_args",
        help="Subcommands to pass to docker-compose. E,g, up, ps, down",
        nargs="+",
    )

    validation_parser = subparsers.add_parser(
        "validation", help="Run validation of the full pipeline"
    )
    validation_parser.add_argument(
        "-s",
        "--start_date",
        type=str,
        help="Date at which to start parsing messages. Default: 7 days ago",
        default="7 days ago_default",
    )
    validation_parser.add_argument(
        "-e",
        "--end_date",
        type=str,
        help="Date at which to start parsing messages. Default: today",
        default="today_default",
    )

    return parser


class EMAPRunner:
    def __init__(self, args: argparse.Namespace, config: GlobalConfiguration):

        self.args = args
        self.config = config

    def setup(self) -> None:
        """Run the setup"""

        repos = self.config.extract_repositories(branch_name=self.args.branch)

        if self.args.init:
            repos.clean()
            repos.clone()

        elif self.args.update:
            repos.update()

        elif self.args.clean:
            return repos.clean()

        elif self.args.only_update_config_from_global:
            pass

        else:
            exit("Please run --help for options")

        self.config.create_or_update_config_dir_from(repos)
        return None

    def docker(self) -> None:
        """Run a docker instance"""

        runner = DockerRunner(main_dir=Path.cwd(), config=self.config)

        if "up" in self.args.docker_compose_args:
            runner.setup_glowroot_password()

        runner.inject_ports()
        runner.run(*self.args.docker_compose_args)

        return None

    def validation(self) -> None:
        """Run a validation run of EMAP"""

        runner = ValidationRunner(
            docker_runner=DockerRunner(main_dir=Path.cwd(), config=self.config),
            time_window=TimeWindow(
                start_date=self.args.start_date, end_date=self.args.end_date
            ),
        )

        runner.run()

        return None

    def run(self, method_name: str) -> None:
        """Call a method of this runner instance defined by its name"""

        if hasattr(self, method_name):
            return getattr(self, method_name)()

        raise EMAPRunnerException(
            f"Failed to run {method_name} as it did not "
            f"exist as a method. Maybe run --help?"
        )


def main():
    parser = create_parser()
    args = parser.parse_args()

    if not Path(args.filename).exists():
        exit(f"Configuration file {args.filename} not found. Exiting")

    runner = EMAPRunner(args=args, config=GlobalConfiguration(args.filename))
    runner.run(args.subcommand)

    return None


if __name__ == "__main__":
    """Invoke with e.g.

        python runner.py setup -i

    to initialise EMAP by cloning all the required repositories and setting
    files with specific environment variables from the global configuration yaml
    """
    main()
