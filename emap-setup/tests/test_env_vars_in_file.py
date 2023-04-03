import os

from datetime import date
from pathlib import Path
from emap_runner.files import EnvironmentFile


this_dir = os.path.dirname(os.path.abspath(__file__))


def test_env_vars_in_file():
    """Ensure that environment variables are read correctly."""

    file = EnvironmentFile(filename=Path(this_dir, "data", "test_env_vars"))
    env_vars = file.environment_variables

    assert env_vars["IDS_CFG_DEFAULT_START_DATETIME"] == "2020-06-04T00:00:00.00Z"
    assert env_vars["BLANK"] == ""
    assert env_vars["UDS_USERNAME"] == r"..=\5%~%%/==~%&1...."


def test_variable_replacement_does_not_change_line_number():
    """Ensure that variable replacement does not change number of lines in file."""

    file_org = EnvironmentFile(filename=Path(this_dir, "data", "test_env_vars"))
    file_repl = EnvironmentFile(filename=Path(this_dir, "data", "test_env_vars"))
    file_repl.replace_value_of("IDS_CFG_DEFAULT_START_DATETIME", str(date.today()))

    assert len(file_org.lines) == len(file_repl.lines)


def test_variable_replacement_does_not_change_variables():
    """Ensure that variable replacement does not change the variable names."""
    file_org = EnvironmentFile(filename=Path(this_dir, "data", "test_env_vars"))
    file_repl = EnvironmentFile(filename=Path(this_dir, "data", "test_env_vars"))
    file_repl.replace_value_of("IDS_CFG_DEFAULT_START_DATETIME", str(date.today()))
    assert (
        file_org.environment_variables.keys() == file_repl.environment_variables.keys()
    )
