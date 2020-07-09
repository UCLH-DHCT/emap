import os
import shutil
import fnmatch

from emap_setup.code_setup.read_config import ReadConfig


class ConfigDirSetup:

    def __init__(self, main_dir: str, config_file: ReadConfig) -> None:
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
                                      }

    def create_or_update_config_dir(self) -> None:
        """
        Create the config directory if it does not exist
        make a copy of each *-config-envs.EXAMPLE file from the repositories
        installed and update the values in these files from the
        global-configuration.yaml file
        :return:
        """
        if not os.path.isdir(self.config_dir):
            os.mkdir(self.config_dir)
        list_of_dirs = os.listdir(self.main_dir)
        for this_dir in list_of_dirs:
            # get list f -envs.EXAMPLE files
            list_of_envs_files = []
            this_dir_full = os.path.join(self.main_dir, this_dir)
            if os.path.isdir(this_dir_full):
                list_of_envs_files = self._get_envs_examples(this_dir_full)

            # create a copy of each file in config dir without .EXAMPLE ext
            for env_file in list_of_envs_files:
                original = os.path.join(self.main_dir, this_dir, env_file)
                new_env_filename = env_file.split('.EX')
                target = os.path.join(self.config_dir, new_env_filename[0])
                shutil.copyfile(original, target)
                # populate the envs files from global-configuration.yaml
                for config_type in self.standard_config_types:
                    self._substitute_info(target, config_type)

    def _get_envs_examples(self, this_dir: str) -> []:
        """
        Return a list of *envs.EXAMPLE files
        :param this_dir: the directory to search
        :return: [] of *envs.EXAMPLE files
        """
        list_of_envs_files = []
        list_of_files = os.listdir(this_dir)
        pattern = "*-envs.EXAMPLE"
        for entry in list_of_files:
            if fnmatch.fnmatch(entry, pattern):
                list_of_envs_files.append(entry)
        return list_of_envs_files

    def _substitute_info(self, filename, config_type):
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
        file = os.path.split(filename)
        element_name_from_file = file[1].split('-config')

        contents = self._get_current_file_contents(filename)
        new_contents = ''
        for line in contents:
            if line.startswith('#'):
                newline = line
            else:
                code = line.split('=')
                fieldname = element_name_from_file[0]
                # unless we are looking up informdb fieldname and config_type
                # should match
                if config_type != 'informdb' and fieldname != config_type:
                    fieldname = config_type
                data = self.config_file.get_data_for(code[0], fieldname,
                                                     config_type)
                if data:
                    newline = '{0}={1}\n'.format(code[0], data)
                else:
                    newline = line
            new_contents = new_contents + newline
        self._write_file_contents(filename, new_contents)

    def _get_current_file_contents(self, filename: str) -> []:
        """
        Get the current contents of file as a list of strings
        :param filename: name of the file to read
        :return: list of contents by line
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()
        return contents

    def _write_file_contents(self, filename: str, contents: []) -> None:
        """
        Write contents to given file
        :param filename: filename to write to
        :param contents: list of contents to write
        :return:
        """
        my_file = open(filename, 'w')
        my_file.write(contents)
        my_file.close()
