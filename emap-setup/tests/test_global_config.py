import os
import pytest

from emap_runner.global_config import GlobalConfiguration

this_dir = os.path.dirname(os.path.abspath(__file__))

config_path = os.path.join(this_dir, "data", "test-global-configuration.yaml")


@pytest.fixture(scope="module")
def config():
    return GlobalConfiguration(config_path)


def test_read_global_config(config: GlobalConfiguration):
    """Configuration should act as a dictionary, populated from a yaml"""

    assert config["main_git_dir"] == "https://github.com/test"


def test_set_global_config(config: GlobalConfiguration):
    """Configuration is immutable"""

    with pytest.raises(Exception):
        config["a"] = "b"


def test_repositories_from_global_config(config: GlobalConfiguration):

    repos = config.repositories

    assert len(repos) > 0
    repo = repos[0]
    assert hasattr(repo, "name")
    assert hasattr(repo, "branch")
