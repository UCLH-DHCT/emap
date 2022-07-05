import sys
import argparse

from typing import Optional, Sequence


class Namespace(argparse.Namespace):
    @property
    def is_up_or_down_a_single_docker_service(self):
        """
        Do the docker_compose_args imply that an operation is operating on a
        single service? i.e. ["up" "hoover"] would be but ["down"] is not.
        """

        args = self.docker_compose_args
        return len(args) > 1 and any(args[-2] == c for c in ("up", "down"))


class Parser(argparse.ArgumentParser):
    def parse_args(self, args: Optional[Sequence[str]] = None) -> Namespace:
        """
        Parse the arguments.

        The docker-compose subcommands need to be intercepted to prevent them
        being interpreted as arguments to this script. For example:

        $ python emap_runner.py docker up -d

        doesn't work without this solution. So parse all the commands up to
        and including docker then set docker_compose_args directly from the
        remainder.
        """
        if args is None:
            args = sys.argv[1:]

        if "docker" not in args:
            return super().parse_args(args)

        idx = next(i + 1 for i, arg in enumerate(args) if arg == "docker")

        namespace = super().parse_args(args[:idx] + ["X"], namespace=Namespace())
        namespace.docker_compose_args = args[idx:]

        return namespace
