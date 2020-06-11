import os
from git import Repo


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir, repos, git_dir):
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param repos: a dictionary items with name and branch of each repositories
        :param git_dir: the main git repository path
        """
        self.main_dir = main_dir
        self.repos = repos
        self.main_github = git_dir

    def clone_necessary_repos(self):
        """
        Clone the requested repositories and branches
        """
        for repo in self.repos:
            this_path = os.path.join(self.main_dir, repo)
            this_git = repo + '.git'
            this_git_path = os.path.join(self.main_github, this_git)
            print('cloning ' + this_git_path + ' branch:' + self.repos[repo]['branch'] + ' to ' + this_path)
            Repo.clone_from(this_git_path, this_path, branch=self.repos[repo]['branch'])
