import os

from os.path import join
from subprocess import Popen, PIPE, CalledProcessError
from typing import List, Optional, IO

from emap_runner.log import logger
from emap_runner.files import File
from emap_runner.utils import EMAPRunnerException


class DockerRunnerException(EMAPRunnerException):
    """Exception for something breaking within docker"""


class DockerRunner:
    """Orchestration for multiple services using docker"""

    def __init__(self, main_dir: str, config: dict):
        """Initialise a docker runner with docker-compose.yml files relative
        to the main directory given a specific configuration"""

        self.main_dir = main_dir
        self.config = config

    def run(
        self,
        *docker_compose_args: str,
        output_filename: Optional[str] = None,
        output_lines: Optional[list] = None,
    ) -> None:
        """
        Run docker compose

        :param docker_compose_args: Arguments to pass to the full docker
                                    compose call e.g. ps
        :param output_filename: Name of the file to write the stdout to
        :param output_lines: List to append the stdout to
        """

        paths = self.docker_compose_paths

        if not all(os.path.exists(path) for path in paths):
            _paths_str = "\n".join(paths)
            raise DockerRunnerException(
                f"Cannot run docker-compose {docker_compose_args}. "
                f"At least one path did not exist:\n {_paths_str} "
            )

        cmd = self.base_docker_compose_command.split()
        for arg in docker_compose_args:
            cmd += [x.strip('"') for x in arg.split()]

        logger.info(f'Running:\n {" ".join(cmd)}\n')

        with Popen(cmd, stdout=PIPE, bufsize=1, universal_newlines=True) as p:

            if output_filename is not None:
                _write_to_file(p.stdout, output_filename)

            elif output_lines is not None:
                _append_to_list(p.stdout, output_lines)

            else:
                _print(p.stdout)

        if p.returncode not in (0, None):
            raise DockerRunnerException(
                f"Process failed with error code: " f"{p.returncode}"
            )

        return None

    @property
    def base_docker_compose_command(self) -> str:
        return (
            "docker-compose -f "
            + " -f ".join(self.docker_compose_paths)
            + f' -p {self.config["EMAP_PROJECT_NAME"]} '
        )

    @property
    def docker_compose_paths(self) -> List[str]:
        """Paths of all the required docker-compose yamls"""

        paths = [
            self.core_docker_compose_path,
            join(self.main_dir, "emap-hl7-processor", "docker-compose.yml"),
            join(self.main_dir, "hoover", "docker-compose.yml"),
        ]

        return paths

    @property
    def core_docker_compose_path(self) -> str:
        return os.path.join(self.main_dir, "Emap-Core", "docker-compose.yml")

    def inject_ports(self) -> None:
        """Inject the required ports into the docker-compose yamls"""

        file = File(self.core_docker_compose_path)
        file.replace("${RABBITMQ_PORT}", self.config["global"]["RABBITMQ_PORT"])
        file.replace(
            "${RABBITMQ_ADMIN_PORT}", self.config["global"]["RABBITMQ_ADMIN_PORT"]
        )
        file.replace(
            "${GLOWROOT_ADMIN_PORT}", self.config["glowroot"]["GLOWROOT_ADMIN_PORT"]
        )

        file.write()
        return None

    def setup_glowroot_password(self) -> None:
        """Run the required command to password protect glowroot"""

        username = self.config["glowroot"]["GLOWROOT_USERNAME"]
        password = self.config["glowroot"]["GLOWROOT_PASSWORD"]

        try:
            self.run(
                'run glowroot-central java -jar "glowroot-central.jar" '
                f"setup-admin-user {username} {password}"
            )

        except CalledProcessError as e:
            raise DockerRunnerException(
                f"{e}\n\n"
                f"Failed to password protect glowroot. Check that the docker "
                f"containers have enough available RAM"
            ) from e

        return None


def _write_to_file(stdout: IO, filename: str) -> None:
    """Write standard output to a file"""

    with open(filename, "w") as file:
        for line in stdout:
            print(line, file=file)

    return None


def _append_to_list(stdout: IO, _list: list) -> None:
    """Append standard output to a list"""

    for line in stdout:
        _list.append(line.decode())

    return None


def _print(stdout: IO) -> None:
    """Print standard output"""

    for line in stdout:
        print(line, end="")

    return None
