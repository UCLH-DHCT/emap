import os

from emap_runner.global_config import GlobalConfiguration
from .utils import work_in_tmp_directory

# Tests the ConfigDirSetup class that creates the config dir

this_dir = os.path.dirname(os.path.abspath(__file__))
data_dir_path = os.path.join(this_dir, "data")


def dir_setup() -> None:
    """Set up the configuration directory"""

    global_config_path = os.path.join(
        this_dir, "data", "test-global-configuration-onlyhl7.yaml"
    )
    config = GlobalConfiguration(global_config_path)
    repos = config.extract_repositories()

    config.create_or_update_config_dir_from(repos)

    return None


@work_in_tmp_directory(to_copy=[data_dir_path])
def test_files_written():
    """Test the files written for config were as expected"""

    dir_setup()
    assert os.path.isdir("config")

    dir1 = os.path.join(data_dir_path, "config_test")  # example test files
    dir2 = "config"  # files written by create_or_update_config_dir() function

    for filename in os.listdir(dir1):
        path1 = os.path.join(dir1, filename)
        path2 = os.path.join(dir2, filename)

        assert os.path.exists(path2)
        assert open(path1, "r").readlines() == open(path2, "r").readlines()
