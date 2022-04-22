import os
import shutil

from tqdm import tqdm

from git import Repo
from git import GitCommandError
from git import InvalidGitRepositoryError

from git import RemoteProgress


class _CloneProgressBar(RemoteProgress):
    def __init__(self):
        super().__init__()
        self.pbar = tqdm()

    def update(self, op_code, cur_count, max_count=None, message=''):
        self.pbar.total = max_count
        self.pbar.n = cur_count
        self.pbar.refresh()


class RepoSetup:
    """Clones the relevant inform repositories.
    """

    def __init__(self, main_dir: str, git_dir: str, repos: dict) -> None:
        """
        Initialise the repository setup
        :param main_dir: the working directory in which to clone repositories
        :param git_dir: the main git repository path
        :param repos: a dictionary items with name
                      and branch of each repositories
        """
        self.main_dir = main_dir
        self.main_github = git_dir
        self.repos = repos
        self.current_repos = self._detect_repos()

    def clone(self) -> None:
        """
        Clone the requested repositories and branches
        and make the self.current_repos list in line
        with these repositories
        """
        self.current_repos = {}
        for repo in self.repos:
            this_path = os.path.join(self.main_dir, repo)
            this_repo = self.repos[repo]
            this_git = this_repo['name'] + '.git'
            this_git_path = os.path.join(self.main_github, this_git)
            print(f'Cloning {this_git_path} branch: {this_repo["branch"]} '
                  f'to {this_path}')

            if os.path.exists(this_repo['name']):
                exit(f"Cannot clone {this_repo}, as it already existed")

            try:
                Repo.clone_from(this_git_path, this_path,
                                branch=this_repo['branch'],
                                progress=_CloneProgressBar())
                self.current_repos[repo] = self.repos[repo].copy()
            except GitCommandError as e:
                exit(f'Necessary repos could not be cloned due to:\n'
                     f'{e}')

    def update(self) -> None:
        for repo in self.repos:
            if self._branches_match(repo):
                try:
                    print(f'Updating {repo:20s} with repo '
                          f'{self.repos[repo]["name"]:20s} and branch '
                          f'{self.repos[repo]["branch"]}')

                    this_repo = Repo(os.path.join(self.main_dir, repo))
                    this_repo.remotes[0].pull()
                except GitCommandError as e:
                    exit(f'Cannot update due to {e}')
            else:
                try:
                    print(f'Checking out repo {repo}, branch '
                          f'{self.repos[repo]["name"]} into '
                          f'{self.repos[repo]["branch"]}')

                    this_repo = Repo(os.path.join(self.main_dir, repo))
                    this_repo.git.checkout(self.repos[repo]['branch'])
                except GitCommandError as e:
                    exit(f'Cannot checkout branch due to {e}')

    def clean(self) -> None:
        """Remove the repositories"""

        for repo in self.repos:

            repo_path = os.path.join(self.main_dir, self.repos[repo]['name'])

            if os.path.exists(repo_path):
                shutil.rmtree(repo_path)

        return None

    def _branches_match(self, repo: str) -> bool:
        """
        Check that the name and branch of the repository match.

        :param repo: string name of repository
        :return: True if match, otherwise False
        """
        match = True
        if repo not in self.current_repos:
            match = False
        elif self.repos[repo]['name'] != self.current_repos[repo]['name']:
            match = False
        elif self.repos[repo]['branch'] != self.current_repos[repo]['branch']:
            match = False
        return match

    def _detect_repos(self) -> dict:
        """
        Repositories already exist; determine the names, directories and
        branches
        :return repos a list of repo dictionaries that reflect the current code
        """
        repos = {}
        list_of_dirs = os.listdir(self.main_dir)
        for this_dir in list_of_dirs:
            try:
                this_repo = Repo(os.path.join(self.main_dir, this_dir))
                remote_url = this_repo.remotes[0].config_reader.get("url")
                repo_name = os.path.splitext(os.path.basename(remote_url))[0]
                repo_info = {
                    'name': repo_name,
                    'branch': this_repo.active_branch.name
                }
                repos[this_dir] = repo_info
            except InvalidGitRepositoryError:
                continue
        return repos
