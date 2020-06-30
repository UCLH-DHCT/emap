from pytest import fixture
from emap_setup.code_setup.read_config import ReadConfig

# Tests the ReadConfig class thatt reads the gloobal configuration file


@fixture(scope="module")
def config_file():
    filename = './data/test-global-configuration.yaml'
    config_file = ReadConfig(filename)
    return config_file


def test_get_git_dir(config_file: ReadConfig):
    """
    Test the get_git_dir function
    :param config_file: ReadConfig class for test configuration
    """
    assert config_file.get_git_dir() == 'https://github.com/test'

def test_get_data_for(config_file: ReadConfig):
    """
    Test data for an entry we know exists
    :param config_file: ReadConfig class for test configuration
    """
    cab = config_file.get_data_for('caboodle')
    assert cab


def test_get_data_for_nonexistent(config_file: ReadConfig):
    """
    Test data for an entry we know does not exist
    :param config_file: ReadConfig class for test configuration
    """
    mydict = config_file.get_data_for('mydict')
    assert not mydict


def test_get_repo_info(config_file: ReadConfig):
    """
    Test data from the repositories section
    :param config_file: ReadConfig class for test configuration
    """
    myrepo = config_file.get_repo_info()
    assert myrepo
    assert len(myrepo) == 2
    assert _check_repo_info(myrepo, 'InformDB')
    assert myrepo['InformDB']['name'] == 'InformDB'
    assert myrepo['InformDB']['branch'] == 'develop'
    assert _check_repo_info(myrepo, 'hl7-vitals')
    assert myrepo['hl7-vitals']['name'] == 'Emap-Core'
    assert myrepo['hl7-vitals']['branch'] == 'vitalsigns'


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
