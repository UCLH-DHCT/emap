from pytest import fixture
import os
import shutil
from filecmp import dircmp
from emap_setup.setup.read_config import ConfigFile
from emap_setup.setup.config_dir_setup import _ConfigDirSetup

# Tests the ConfigDirSetup class that creates the config dir


@fixture(scope="module")
def dir_setup():
    """
    returns: Instance of ConfigDirSetup to use in tests
    """
    filename = './data/test-global-configuration.yaml'
    config_file = ConfigFile(filename)
    dir_setup = _ConfigDirSetup(os.path.join(os.getcwd(), 'data'), config_file)
    if os.path.isdir('./data/config'):
        _clean_up()
    return dir_setup


def test_create_config_dir(dir_setup : _ConfigDirSetup):
    """
    tests that the config directory is created
    :param dir_setup:
    """
    assert not os.path.isdir('./data/config')
    dir_setup.create_or_update()
    assert os.path.isdir('./data/config')


def test_files_written():
    """
    Test the files written for config were as expected
    """
    dir1 = './data/config_test'  # example test files
    dir2 = './data/config'  # files written by create_or_update_config_dir() function
    different = dircmp(dir1, dir2).diff_files
    assert len(different) == 0
    _clean_up()


def _clean_up():
    shutil.rmtree('./data/config')


