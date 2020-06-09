import os
from git import Repo


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir, repos):
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param repos: a list containing dictionary items with name and branch of each repositories
        """
        self.main_dir = main_dir
        self.repos = repos

        self.main_github = 'https://github.com/inform-health-informatics'

    def clone_necessary_repos(self):
        """
        Clone the requested repositories and branches
        :return:
        """
        for repo in self.repos:
            this_path = os.path.join(self.main_dir, repo['name'])
            this_git = repo['name'] + '.git'
            this_git_path = os.path.join(self.main_github, this_git)
            print('cloning ' + this_git_path + ' branch:' + repo['branch'] + ' to ' + this_path)
            Repo.clone_from(this_git_path, this_path, branch=repo['branch'])
