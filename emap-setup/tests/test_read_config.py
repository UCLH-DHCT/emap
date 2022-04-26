import pytest

from pytest import fixture
from emap_runner.read_config import ConfigFile

# Tests the ReadConfig class that reads the global configuration file


@fixture(scope="module")
def config_file():
    filename = './data/test-global-configuration.yaml'
    config_file = ConfigFile(filename)
    return config_file


def test_get_git_dir(config_file: ConfigFile):
    """
    Test the get_git_dir function
    :param config_file: ReadConfig class for test configuration
    """
    assert config_file.git_dir == 'https://github.com/test'


def test_get_data_for(config_file: ConfigFile):
    """
    Test data for an entry we know exists
    :param config_file: ReadConfig class for test configuration
    """
    cab = config_file.get_data_for('caboodle', 'element')
    assert cab


def test_get_data_for_nonexistent(config_file: ConfigFile):
    """
    Test data for an entry we know does not exist
    :param config_file: ReadConfig class for test configuration
    """

    with pytest.raises(KeyError):
        _ = config_file.get_data_for('mydict', 'element')


def test_get_repo_info(config_file: ConfigFile):
    """
    Test data from the repositories section
    :param config_file: ReadConfig class for test configuration
    """
    repo_info = config_file.repo_info

    assert repo_info
    assert len(repo_info) == 2
    assert _check_repo_info(repo_info, 'InformDB')
    assert repo_info['InformDB']['name'] == 'InformDB'
    assert repo_info['InformDB']['branch'] == 'develop'
    assert _check_repo_info(repo_info, 'hl7-vitals')
    assert repo_info['hl7-vitals']['name'] == 'Emap-Core'
    assert repo_info['hl7-vitals']['branch'] == 'vitalsigns'


def _check_repo_info(myrepo: dict, myname: str) -> bool:
    """
    Check that myname occurs in the dict myrepo and has relevant
    entries
    :param myrepo: dict containing the repository entries
    :param myname: str name to check
    :return: True if myrepo[myname] exists and has relevant fields,
    False otherwise
    """
    if myname not in myrepo:
        return False
    elif len(myrepo[myname]) != 2:
        return False
    elif 'name' not in myrepo[myname]:
        return False
    elif 'branch' not in myrepo[myname]:
        return False
    return True
