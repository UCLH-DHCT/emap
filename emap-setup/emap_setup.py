import sys
from repo_setup import RepoSetup


def create_repostories():
    r_setup = RepoSetup('init')
    r_setup.clone_necessary_repos()


def main(args):
    create_repostories()
    print("All done")


if __name__ == '__main__':
    main(sys.argv)
