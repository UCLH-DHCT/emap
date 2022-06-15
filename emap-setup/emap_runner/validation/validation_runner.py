from subprocess import Popen
from datetime import date, timedelta
from time import time, sleep
from typing import Union
from pathlib import Path

from emap_runner.files import EnvironmentFile
from emap_runner.utils import EMAPRunnerException


class ValidationRunnerException(EMAPRunnerException):
    """Exception for when a validation issue occurs"""


class ValidationRunner:
    def __init__(self, docker_runner: "DockerRunner", time_window: "TimeWindow"):
        """Validation runner that will be run over a time window"""

        self.start_time = None
        self.timeout = timedelta(hours=10)

        self.docker = docker_runner
        self.time_window = time_window

    def run(self) -> None:
        """Run a validation run pipeline by constructing services and waiting
        until all message queues are empty"""

        with TemporaryEnvironmentState(self.env_dir_path):
            self._set_time_window_in_envs()
            self._run_emap()
            self._wait_for_queue_to_empty()
            self._save_logs_and_stop()

        return None

    @property
    def env_dir_path(self) -> Path:
        return Path(self.docker.main_dir, "config")

    @property
    def log_file_prefix(self) -> Path:
        """Common prefix used for all log files"""
        return Path(self.logs_directory, f"rebuild_log_{date.today()}")

    @property
    def logs_directory(self) -> Path:
        return Path(self.docker.main_dir, "validation_logs")

    def _create_logs_directory(self) -> None:
        """Create a directory just for logging"""

        if not self.logs_directory.exists():
            self.logs_directory.mkdir()

        return None

    def _set_time_window_in_envs(self) -> None:
        """Set the time window in all the required files"""

        for item in self.env_dir_path.iterdir():
            if str(item).startswith(".") or not str(item).endswith("-envs"):
                continue

            self._set_time_window_in_env_file(
                EnvironmentFile(Path(self.env_dir_path, item))
            )

        return None

    def _set_time_window_in_env_file(self, file: EnvironmentFile) -> None:
        """Set the correct time stamps in the environment file"""

        if not self.time_window.start.is_default:

            file.replace_value_of(
                "IDS_CFG_DEFAULT_START_DATETIME", self.time_window.start_stamp
            )
            file.replace_value_of("HOOVER_DATE_FROM", self.time_window.start_stamp)

        if not self.time_window.end.is_default:
            file.replace_value_of("IDS_CFG_END_DATETIME", self.time_window.end_stamp)
            file.replace_value_of("HOOVER_DATE_UNTIL", self.time_window.end_stamp)

        file.write(directory=self.env_dir_path)

        return None

    def _run_emap(self) -> None:
        """Run the services that constitute EMAP"""
        self._create_logs_directory()

        self.docker.inject_ports()
        self.docker.run("down")
        self.docker.run("up --build -d cassandra rabbitmq")
        self.docker.setup_glowroot_password()
        self.docker.run("up --build -d glowroot-central")
        self.docker.run("ps")

        """
        Running emapstar before data sources requires RabbitMQ to be up and running. A time delay was added to overcome
        the time delay required for RabbitMQ to be there. Should this proof not to be sufficient, then the running order
        may need to change, i.e. the data sources to be started first (as background services though!).
        """
        sleep(180)
        _ = Popen(
            self.docker.base_docker_compose_command.split()
            + ["up", "--build", "-d", "emapstar"]
        )

        self.docker.run(
            "up --build --exit-code-from hl7source hl7source",
            output_filename=f"{self.log_file_prefix}_hl7source.txt",
        )
        self.docker.run(
            "up --build --exit-code-from hoover hoover",
            output_filename=f"{self.log_file_prefix}_hoover.txt",
        )

        self.docker.run("ps")

    def _wait_for_queue_to_empty(self) -> None:
        """
        Wait for the rabbitmq queue to be empty
        If it's still going after 10 hours something's gone very wrong and we
        should give up
        """
        self.start_time = time()

        while self._has_populated_queues:

            sleep(120)

            if self._exceeded_timeout:
                self._save_logs_and_stop()
                raise ValidationRunnerException(
                    f"Waiting for queue timed out. Elapsed time "
                    f"({self._elapsed_time}) > timeout ({self.timeout})"
                )

        # exits too keenly from databaseExtracts queue, adding in a wait period
        sleep(600)

        return None

    @property
    def _exceeded_timeout(self) -> bool:
        return self._elapsed_time > self.timeout

    @property
    def _elapsed_time(self) -> timedelta:
        """Seconds elapsed since the runner started"""
        return timedelta(microseconds=time() - self.start_time)

    @property
    def _has_populated_queues(self) -> bool:
        """Are there queues that are still populated?

        Check the number of messages remaining in the rabbitmmq queue and parse
        an output like:

            name    messages
            hl7Queue        0
            databaseExtracts        0
        """
        output_lines = []

        self.docker.run(
            "exec rabbitmq rabbitmqctl -q list_queues", output_lines=output_lines
        )

        def n_messages(_line):
            return int(_line.split()[1])

        return all(n_messages(line) == 0 for line in output_lines[1:])

    def _save_logs_and_stop(self) -> None:
        """Save the logs of the required docker containers"""

        self.docker.run("ps")
        for name in ("emapstar", "rabbitmq"):
            self.docker.run(
                f"logs {name}", output_filename=f"{self.log_file_prefix}_{name}.txt"
            )

        self.docker.run("down")
        self.docker.run("ps")

        return None


class TemporaryEnvironmentState:
    def __init__(self, dir_path: Union[Path, str]):
        """
        Context manager for a temporary environment state for which all env
        files are initially cached then re-written

        :param: dir_path: Path to the directory containing XXX-config-envs file
        """

        self._files = {}

        for filename in Path(dir_path).iterdir():
            file_path = Path(dir_path, filename)
            self._files[file_path] = open(file_path, "r").readlines()

    def __enter__(self):
        return self._files

    def __exit__(self, *args, **kwargs):

        for file_path, file_lines in self._files.items():
            with open(file_path, "w") as file:
                file.write("".join(file_lines))
