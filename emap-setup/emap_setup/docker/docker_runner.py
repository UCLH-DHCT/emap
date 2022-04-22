import os
import subprocess
from typing import List


class DockerRunner:
    """Orchestration for multiple services using docker"""

    def __init__(self,
                 main_dir:      str,
                 config:        dict,
                 use_fake_epic: bool):
        """Initialise a docker runner with docker-compose.yml files relative
        to the main directory given a specific configuration"""

        self.main_dir = main_dir
        self.config = config
        self.use_fake_epic = use_fake_epic

    def run(self, *docker_compose_args: str) -> None:
        """Run docker compose"""

        paths = self.docker_compose_paths

        if not all(os.path.exists(path) for path in paths):
            _paths_str = "\n".join(paths)
            exit(f'Cannot run docker-compose {docker_compose_args}. '
                 f'At least one path did not exist:\n {_paths_str} ')

        subprocess.run('docker-compose -f ' + ' -f '.join(paths)
                       + f' -p {self.config["EMAP_PROJECT_NAME"]} '
                       + ' '.join(docker_compose_args),
                       shell=True,
                       check=True)

        return None

    @property
    def docker_compose_paths(self) -> List[str]:
        """Paths of all the required docker-compose yamls"""
        from os.path import join

        paths = [self.core_docker_compose_path,
                 join(self.main_dir, 'emap-hl7-processor', 'docker-compose.yml'),
                 join(self.main_dir, 'hoover', 'docker-compose.yml')]

        if self.use_fake_epic:
            paths.append(
                join(self.main_dir, 'hoover', 'docker-compose.fake_services.yml')
            )

        return paths

    @property
    def core_docker_compose_path(self) -> str:
        return os.path.join(self.main_dir, 'Emap-Core', 'docker-compose.yml')

    def inject_ports(self) -> None:
        """Inject the required ports into the docker-compose yamls"""

        file = File(self.core_docker_compose_path)
        file.replace('${RABBITMQ_PORT}', self.config['global']['RABBITMQ_PORT'])
        file.replace('${RABBITMQ_ADMIN_PORT}', self.config['global']['RABBITMQ_ADMIN_PORT'])
        file.replace('${GLOWROOT_ADMIN_PORT}', self.config['glowroot']['GLOWROOT_ADMIN_PORT'])

        file.write()
        return None

    def setup_glowroot_password(self) -> None:
        """Run the required command to password protect glowroot"""

        username = self.config['glowroot']['GLOWROOT_USERNAME']
        password = self.config['glowroot']['GLOWROOT_PASSWORD']

        try:
            self.run('run glowroot-central java -jar "glowroot-central.jar" '
                     f'setup-admin-user {username} {password}')

        except subprocess.CalledProcessError as e:
            exit(f'{e}\n\n'
                 f'Failed to password protect glowroot. Check that the docker '
                 f'containers have enough available RAM')

        return None


class File:

    def __init__(self, filename):

        self.filename = filename
        self.lines = open(filename, 'r').readlines()

    def replace(self, old: str, new: str) -> None:
        """Replace a string with another that may or may not exist"""

        for i, line in enumerate(self.lines):
            if str(old) in line:
                self.lines[i] = line.replace(str(old), str(new))

        return None

    def write(self) -> None:
        """Write the file lines to a new version of the file"""

        with open(self.filename, 'w') as file:
            file.write(''.join(self.lines))

        return None
