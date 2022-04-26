import os
import fnmatch
from datetime import datetime

from emap_runner.files import EnvironmentFile, NewLine, CommentLine
from emap_runner.read_config import ConfigFile


def create_or_update_config_dir(main_dir:    str,
                                config_file: ConfigFile) -> None:
    """
    Create the config dir populating and copying any envs files
    from the repositories present

    :param main_dir: Directory to work in
    :param config_file: Name of the configuration file
    """
    print('Updating config/ directory')

    config_dir_setup = _ConfigDirSetup(main_dir, config_file)
    config_dir_setup.create_or_update()

    return None


class _ConfigDirSetup:

    def __init__(self, main_dir: str, config_file: ConfigFile) -> None:
        # config directory next to repositories
        self.main_dir = main_dir
        self.config_dir = os.path.join(main_dir, 'config')
        self.config_file = config_file
        self.standard_config_types = {
            'rabbitmq',
            'ids',
            'uds',
            'informdb',
            'caboodle',
            'omop',
            'dates',
            'global',
            'glowroot'
                                      }

    def create_or_update(self) -> None:
        """
        Create the config directory if it does not exist
        make a copy of each *-config-envs.EXAMPLE file from the repositories
        installed and update the values in these files from the
        global-configuration.yaml file
        :return:
        """
        if not os.path.isdir(self.config_dir):
            os.mkdir(self.config_dir)

        for dir_name in self._non_config_dirs:

            # get list f -envs.EXAMPLE files
            list_of_envs_files = []
            dir_path = os.path.join(self.main_dir, dir_name)
            if os.path.isdir(dir_path):
                list_of_envs_files = self._get_envs_examples(dir_path)

            # create a copy of each file in config dir without .EXAMPLE ext
            for env_filename in list_of_envs_files:

                file = EnvironmentFile.from_example_file(
                    os.path.join(dir_path, env_filename)
                )

                for config_type in self.standard_config_types:
                    self._substitute_info(file, config_type)

                file.write(directory=self.config_dir)

                for line in file.unchanged_lines:
                    print(f'WARNING: {line.strip()[:29]:30s} was not updated '
                          f'from {self.config_file.filename}')

    @property
    def _non_config_dirs(self) -> list:
        return list(filter(lambda n: n != 'config', os.listdir(self.main_dir)))

    @staticmethod
    def _get_envs_examples(this_dir: str) -> list:
        """
        Return a list of *envs.EXAMPLE files
        :param this_dir: the directory to search
        :return: List of *envs.EXAMPLE files
        """
        list_of_envs_files = []
        list_of_files = os.listdir(this_dir)
        pattern = "*-envs.EXAMPLE"
        for entry in list_of_files:
            if fnmatch.fnmatch(entry, pattern):
                list_of_envs_files.append(entry)
        return list_of_envs_files

    def _substitute_info(self,
                         file:        EnvironmentFile,
                         config_type: str
                         ) -> None:
        """
        Here we are looping through the standard entries in the configuration
        We check that the name of the envs file matches one of these
        entries with allows straightforward look up in the config dictionary
        which will have entries
            ids:
                IDS_USERNAME: uname
                etc.

        For informdb the entries maybe different for different configurations
        e.g. Emap-Core and OMOP so we need look these up separately
            informdb:
                Emap-Core:
                  INFORMDB_SCHEMA: inform_schema
                OMOP-ETL:
                  INFORMDB_SCHEMA: inform_schema_for_omop

        :param filename:
        :param config_type:
        :return:
        """

        # get name of the name-config-envs file
        fieldname = os.path.basename(file.filename).split('-config')[0]

        if config_type != 'informdb' and fieldname != config_type:
            fieldname = config_type

        for i, line in enumerate(file.lines):

            if line.startswith('#'):
                file.lines[i] = CommentLine(line)
                continue

            key = line.split('=')[0]

            try:
                data = self.config_file.get_data_for(dataname=key,
                                                     element=fieldname,
                                                     outer_element=config_type)

                if isinstance(data, datetime):
                    datestr = data.strftime('%Y-%m-%dT%H:%M:%S.%zZ')
                    data = datestr[0:20] + datestr[21:23] + 'Z'
                file.lines[i] = NewLine(f'{key}={data}\n')

            except KeyError:
                continue

        return None
