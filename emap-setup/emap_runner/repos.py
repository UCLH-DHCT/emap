import os.path
from typing import Optional


class Repository:
    def __init__(self, name: str, branch: Optional[str] = None):

        self.name = name
        self.branch = branch

    def clone(self) -> None:
        raise NotImplementedError

    def update(self) -> None:
        raise NotImplementedError

    def clean(self) -> None:
        raise NotImplementedError

    @property
    def path(self) -> str:
        """Path to this repository"""
        return os.path.join(os.getcwd(), self.name)


class Repositories(list):
    def update(self) -> None:
        return self._run_for_all("update")

    def clone(self) -> None:
        return self._run_for_all("clone")

    def clean(self) -> None:
        return self._run_for_all("clean")

    def _run_for_all(self, method_name: str) -> None:

        for repo in self:
            getattr(repo, method_name)()

        return None
