from pytest import fixture
import os
from emap_setup.code_setup.read_config import ReadConfig
from emap_setup.code_setup.config_dir_setup import ConfigDirSetup

# Tests the ConfigDirSetup class that creates the config dir


@fixture(scope="module")
def dir_setup():
    filename = './data/test-global-configuration.yaml'
    config_file = ReadConfig(filename)
    dir_setup = ConfigDirSetup(os.path.join(os.getcwd(), 'data'), config_file)
    return dir_setup


def test_create_config_dir(dir_setup):
 #   assert not os.path.isdir('./data/config')
    dir_setup.create_or_update_config_dir()
    assert os.path.isdir('./data/config')
