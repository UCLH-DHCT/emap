import sys

from os.path import dirname, abspath, join, exists
from emap_runner.runner import create_parser, EMAPRunner
from emap_runner.global_config import GlobalConfiguration
from .utils import work_in_tmp_directory

data_dir_path = join(dirname(abspath(__file__)), "data")

config_filepath = join(data_dir_path, "test-global-configuration-only-emap-setup.yaml")


@work_in_tmp_directory(to_copy=None)
def test_clone_then_clean_repos():

    parser = create_parser()
    config = GlobalConfiguration(config_filepath)

    runner = EMAPRunner(args=parser.parse_args(["setup", "-i"]), config=config)
    runner.run("setup")

    # Ensure that the cloned directory exists
    assert exists("emap-setup")
    assert exists("config")

    # and can be cleaned
    runner = EMAPRunner(args=parser.parse_args(["setup", "-c"]), config=config)
    runner.run("setup")

    assert not exists("emap-setup")
