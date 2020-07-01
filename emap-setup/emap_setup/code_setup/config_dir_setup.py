import os
import shutil
import fnmatch

from emap_setup.code_setup.read_config import ReadConfig


class ConfigDirSetup:

    def __init__(self, main_dir: str, config_file: ReadConfig) -> None:
        # config directory next to repositories
        self.main_dir = main_dir
        self.config_dir = os.path.join(main_dir, 'config')

        # save the different categories of data
        self.caboodle_data = config_file.get_data_for('caboodle')
        self.uds_data = config_file.get_data_for('uds')
        self.rabbitmq_data = config_file.get_data_for('rabbitmq')
        self.informdb_data = config_file.get_data_for('informdb')
        self.ids_data = config_file.get_data_for('ids')
        self.dates_data = config_file.get_data_for('dates')
        self.omop_data = config_file.get_data_for('omop')

    def create_or_update_config_dir(self):
        if not os.path.isdir(self.config_dir):
            os.mkdir(self.config_dir)
        list_of_dirs = os.listdir(self.main_dir)
        for this_dir in list_of_dirs:
            list_of_envs_files = []
            if os.path.isdir(this_dir):
                list_of_envs_files = self._get_envs_examples(this_dir)

            for env_file in list_of_envs_files:
                original = os.path.join(self.main_dir, this_dir, env_file)
                new_env_filename = env_file.split('.EX')
                target = os.path.join(self.config_dir, new_env_filename[0])
                shutil.copyfile(original, target)
                self._substitute_caboodle_info(target)
                self._substitute_uds_info(target)
                self._substitute_rabbitmq_info(target)
                self._substitute_ids_info(target)
                self._substitute_informdb_info(target, this_dir)
                self._substitute_omop_info(target)

    def _get_envs_examples(self, this_dir: str) -> []:
        """
        Return a list of *envs.EXAMPLE files
        :param this_dir: the directory to search
        :return:
        """
        list_of_envs_files = []
        list_of_files = os.listdir(os.path.join(self.main_dir, this_dir))
        pattern = "*-envs.EXAMPLE"
        for entry in list_of_files:
            if fnmatch.fnmatch(entry, pattern):
                list_of_envs_files.append(entry)
        return list_of_envs_files

    def _substitute_caboodle_info(self, filename):
        """
        Substitute values for caboodle environment
        :param filename: file in which substitution should happen
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('CABOODLE'):
                code = line.split('=')
                if 'URL' in code[0]:
                    newline = code[0] + '=' + self.caboodle_data['jdbc_url'] \
                              + '\n'
                elif 'USER' in code[0]:
                    newline = code[0] + '=' + self.caboodle_data['username'] \
                              + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' + self.caboodle_data['password'] \
                              + '\n'
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN\n'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()

    def _substitute_uds_info(self, filename):
        """
        Substitute values for UDS environment
        :param filename: file in which substitution should happen
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('UDS'):
                code = line.split('=')
                if 'URL' in code[0]:
                    newline = code[0] + '=' + self.uds_data['jdbc_url'] + '\n'
                elif 'USER' in code[0]:
                    newline = code[0] + '=' + self.uds_data['username'] + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' + self.uds_data['password'] + '\n'
                elif 'SCHEMA' in code[0]:
                    newline = code[0] + '=' + self.uds_data['schema'] + '\n'
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()

    def _substitute_rabbitmq_info(self, filename):
        """
        Substitute values for rabbitmq environment
        :param filename: file in which substitution should happen
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('SPRING_RABBITMQ'):
                code = line.split('=')
                if 'HOST' in code[0]:
                    newline = code[0] + '=' + self.rabbitmq_data['host'] + '\n'
                elif 'USER' in code[0]:
                    newline = code[0] + '=' + self.rabbitmq_data['username'] \
                              + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' + self.rabbitmq_data['password'] \
                              + '\n'
                elif 'PORT' in code[0]:
                    newline = code[0] + '={0}\n'
                    newline = newline.format(self.rabbitmq_data['spring_port'])
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            elif line.startswith('RABBITMQ'):
                code = line.split('=')
                if 'ADMIN_PORT' in code[0]:
                    newline = code[0] + '={0}\n'
                    newline = newline.format(self.rabbitmq_data['admin_port'])
                elif 'PORT' in code[0]:
                    newline = code[0] + '={0}\n'
                    newline = newline.format(self.rabbitmq_data['port'])
                elif 'USER' in code[0]:
                    newline = code[0] + '=' + self.rabbitmq_data['username'] \
                              + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' + self.rabbitmq_data['password'] \
                              + '\n'
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()

    def _substitute_ids_info(self, filename):
        """
        Substitute values for IDS environment
        :param filename: file in which substitution should happen
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('IDS'):
                code = line.split('=')
                if 'URL' in code[0]:
                    newline = code[0] + '=' + self.ids_data['jdbc_url'] + '\n'
                elif 'USER' in code[0]:
                    newline = code[0] + '=' + self.ids_data['username'] + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' + self.ids_data['password'] + '\n'
                elif 'SCHEMA' in code[0]:
                    newline = code[0] + '=' + self.ids_data['schema'] + '\n'
                elif 'START_DATETIME' in code[0]:
                    if self.dates_data['start'] is None:
                        newline = code[0] + '=\n'
                    else:
                        newline = code[0] + '={0}\n'
                        newline = newline.format(self.dates_data['start'])
                elif 'END_DATETIME' in code[0]:
                    if self.dates_data['end'] is None:
                        newline = code[0] + '=\n'
                    else:
                        newline = code[0] + '={0}\n'
                        newline = newline.format(self.dates_data['end'])
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()

    def _substitute_informdb_info(self, filename, repo):
        """
        Substitute values for informDB environment
        :param filename: file in which substitution should happen
        :param repo: repo using informDB
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('INFORMDB'):
                code = line.split('=')
                if 'URL' in code[0]:
                    newline = code[0] + '=' \
                              + self.informdb_data[repo]['jdbc_url'] + '\n'
                elif 'USER' in code[0]:
                    newline = code[0] + '=' \
                              + self.informdb_data[repo]['username'] + '\n'
                elif 'PASS' in code[0]:
                    newline = code[0] + '=' \
                              + self.informdb_data[repo]['password'] + '\n'
                elif 'SCHEMA' in code[0]:
                    newline = code[0] + '=' \
                              + self.informdb_data[repo]['schema'] + '\n'
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()

    def _substitute_omop_info(self, filename):
        """
        Substitute values for OMOP environment
        :param filename: file in which substitution should happen
        """
        my_file = open(filename)
        contents = my_file.readlines()
        my_file.close()

        new_contents = ''
        for line in contents:
            if line.startswith('OPS'):
                code = line.split('=')
                if 'DROP' in code[0]:
                    newline = code[0] + '={0}\n'
                    newline = newline.format(self.omop_data['drop_and_create'])
                elif 'SCHEMA' in code[0]:
                    newline = code[0] + '=' + self.omop_data['schema'] + '\n'
                else:
                    print('Unknown configuration element ' + code[0]
                          + ' found')
                    newline = code[0] + '=PLEASE FILL IN'
            else:
                newline = line
            new_contents = new_contents + newline

        my_file = open(filename, 'w')
        my_file.write(new_contents)
        my_file.close()
