import os
import shutil
import tempfile

from emap_setup.validation.validation_runner import TemporaryEnvironmentState


def test_tmp_env_state_caches_env():

    dir_path = tempfile.mkdtemp()

    def write_to_file(string):
        with open(os.path.join(dir_path, 'a_file'), 'w') as file:
            print(string, file=file)

    def file_content():
        return ''.join(open(os.path.join(dir_path, 'a_file'), 'r').readlines())

    write_to_file('a')
    with TemporaryEnvironmentState(dir_path):
        write_to_file('b')
        assert file_content() == 'b\n'

    assert file_content() == 'a\n'

    shutil.rmtree(dir_path)
