from pytest import fixture
from emap_runner.runner import create_parser


@fixture(scope="module")
def arg_parser():
    """
    :return: instance of ArgumentParser to be used by tests
    """
    arg_parser = create_parser()
    return arg_parser

# TODO: what should be tested here?
# (I don't think its worth testing argparse functionality)
