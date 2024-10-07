from unittest.mock import patch, MagicMock

import pytest

from os import mkdir
from os.path import dirname, abspath, exists
from pathlib import Path
from datetime import date

from emap_runner.runner import create_parser, EMAPRunner
from emap_runner.validation.validation_runner import (
    ValidationRunner,
    TemporaryEnvironmentState,
)
from emap_runner.docker.docker_runner import DockerRunner
from emap_runner.global_config import GlobalConfiguration
from emap_runner.utils import TimeWindow
from .utils import work_in_tmp_directory

config_path_only_docs = Path(
    dirname(abspath(__file__)), "data", "test-global-configuration-only-docs.yaml"
)
config_path_all = Path(
    dirname(abspath(__file__)), "data", "test-global-configuration.yaml"
)


@work_in_tmp_directory(to_copy=None)
def test_clone_then_clean_repos():

    parser = create_parser()
    config = GlobalConfiguration(config_path_only_docs)

    runner = EMAPRunner(args=parser.parse_args(["setup", "-i"]), config=config)
    runner.run()

    # Ensure that the cloned directory exists
    assert exists("emap_documentation")
    assert exists("config")

    # and can be cleaned
    runner = EMAPRunner(args=parser.parse_args(["setup", "-c"]), config=config)
    runner.run()

    assert not exists("emap_documentation")


def test_default_time_window():

    parser = create_parser()
    args = parser.parse_args(["validation"])

    window = TimeWindow(start_date=args.start_date, end_date=args.end_date)
    assert window.end == date.today()
    assert window.start.toordinal() == date.today().toordinal() - 7


@work_in_tmp_directory(to_copy=[config_path_only_docs])
def test_time_window_is_set():

    config = GlobalConfiguration(Path("test-global-configuration-only-docs.yaml"))

    runner = ValidationRunner(
        docker_runner=DockerRunner(project_dir=Path.cwd(), config=config),
        time_window=TimeWindow(start_date="today", end_date="7 days ago"),
    )

    mkdir("config")
    with open("config/hoover-envs", "w") as file:
        print("HOOVER_DATE_FROM=X\n" "HOOVER_DATE_UNTIL=Y", file=file)

    with TemporaryEnvironmentState(runner.env_dir_path):
        runner._set_time_window_in_envs()

        with open("config/hoover-envs", "r") as file:
            for line in file:
                if line.startswith("HOOVER_DATE_"):
                    assert line.strip().endswith("T00:00:00.00Z")


@pytest.mark.parametrize(
    "args_list,use_hl7,use_hoover,use_waveform",
    [
        ([], True, True, False),
        (["--no-use-hoover"], True, False, False),
        (["--no-use-hl7"], False, True, False),
        (["--use-waveform"], True, True, True),

    ])
def test_validation_source_arguments_set_correct_runner_attributes(args_list, use_hl7, use_hoover, use_waveform):
    """Test source parser arguments translate to correct runner attributes"""
    parser = create_parser()

    args = parser.parse_args(["validation", *args_list])
    global_config = GlobalConfiguration(config_path_all)

    with patch.object(ValidationRunner, 'run', new_callable=MagicMock) as mock_obj:
        emap_runner = EMAPRunner(args=args, config=global_config)
        validation_runner = emap_runner.run()
        mock_obj.assert_called_once()
        assert validation_runner.use_hl7_reader == use_hl7
        assert validation_runner.use_hoover == use_hoover
        assert validation_runner.use_waveform == use_waveform


@pytest.mark.parametrize("queue_length,expected", [(0, True), (1, False)])
def test_zero_length_queues(queue_length, expected):
    """Test that stdout from a rabbitmq list queues call is parsed correctly"""

    lines = [
        "\n",
        "name    messages\n",
        f"databaseExtracts        {queue_length}\n",
        "hl7Queue        0\n",
    ]

    method = ValidationRunner._stdout_rabbitmq_queues_all_zero_length
    assert method(lines) == expected
