from pytest import fixture

from emap_setup import define_arguments

@fixture(scope="module")
def arg_parser():
    arg_parser = define_arguments()
    return arg_parser

def test_help(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[0].dest == 'help'

def test_init(arg_parser):
    assert arg_parser._mutually_exclusive_groups[0]._actions[1].dest == 'init'
