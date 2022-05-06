from os.path import dirname, abspath, exists
from pathlib import Path

from emap_runner.runner import create_parser, EMAPRunner
from emap_runner.global_config import GlobalConfiguration
from .utils import work_in_tmp_directory

config_path = Path(
    dirname(abspath(__file__)), "data", "test-global-configuration-only-docs.yaml"
)


@work_in_tmp_directory(to_copy=None)
def test_clone_then_clean_repos():

    parser = create_parser()
    config = GlobalConfiguration(config_path)

    runner = EMAPRunner(args=parser.parse_args(["setup", "-i"]), config=config)
    runner.run("setup")

    # Ensure that the cloned directory exists
    assert exists("emap_documentation")
    assert exists("config")

    # and can be cleaned
    runner = EMAPRunner(args=parser.parse_args(["setup", "-c"]), config=config)
    runner.run("setup")

    assert not exists("emap_documentation")
